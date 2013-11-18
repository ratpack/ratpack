/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.server.internal;

import com.google.common.util.concurrent.ListeningExecutorService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import ratpack.error.ClientErrorHandler;
import ratpack.error.ServerErrorHandler;
import ratpack.error.internal.DefaultClientErrorHandler;
import ratpack.error.internal.DefaultServerErrorHandler;
import ratpack.error.internal.ErrorCatchingHandler;
import ratpack.file.FileRenderer;
import ratpack.file.FileSystemBinding;
import ratpack.file.MimeTypes;
import ratpack.file.internal.*;
import ratpack.handling.Context;
import ratpack.handling.EventHandler;
import ratpack.handling.Handler;
import ratpack.handling.Redirector;
import ratpack.handling.internal.*;
import ratpack.http.MutableHeaders;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.http.internal.*;
import ratpack.launch.LaunchConfig;
import ratpack.registry.Registry;
import ratpack.registry.RegistryBuilder;
import ratpack.render.CharSequenceRenderer;
import ratpack.render.internal.DefaultCharSequenceRenderer;
import ratpack.server.BindAddress;
import ratpack.server.PublicAddress;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Date;

import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

@ChannelHandler.Sharable
public class NettyHandlerAdapter extends SimpleChannelInboundHandler<FullHttpRequest> {

  private final Handler handler;
  private final Handler return404;
  private final ListeningExecutorService blockingExecutorService;

  private Registry registry;

  public NettyHandlerAdapter(Handler handler, LaunchConfig launchConfig, ListeningExecutorService blockingExecutorService) {
    this.blockingExecutorService = blockingExecutorService;
    this.handler = new ErrorCatchingHandler(handler);
    this.return404 = new ClientErrorForwardingHandler(NOT_FOUND.code());

    this.registry = RegistryBuilder.builder()
      // If you update this list, update the class level javadoc on Context.
      .add(FileSystemBinding.class, new DefaultFileSystemBinding(launchConfig.getBaseDir()))
      .add(MimeTypes.class, new ActivationBackedMimeTypes())
      .add(PublicAddress.class, new DefaultPublicAddress(launchConfig.getPublicAddress(), launchConfig.getSSLContext() == null ? "http" : "https"))
      .add(Redirector.class, new DefaultRedirector())
      .add(ClientErrorHandler.class, new DefaultClientErrorHandler())
      .add(ServerErrorHandler.class, new DefaultServerErrorHandler())
      .add(LaunchConfig.class, launchConfig)
      .add(FileRenderer.class, new DefaultFileRenderer())
      .add(CharSequenceRenderer.class, new DefaultCharSequenceRenderer())
      .build();
  }

  public void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest nettyRequest) throws Exception {
    if (!nettyRequest.getDecoderResult().isSuccess()) {
      sendError(ctx, HttpResponseStatus.BAD_REQUEST);
      return;
    }

    final FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

    final Request request = new DefaultRequest(new NettyHeadersBackedHeaders(nettyRequest.headers()), nettyRequest.getMethod().name(), nettyRequest.getUri(), nettyRequest.content());

    final Channel channel = ctx.channel();

    final DefaultStatus responseStatus = new DefaultStatus();
    final MutableHeaders responseHeaders = new NettyHeadersBackedMutableHeaders(nettyResponse.headers());
    final ByteBuf responseBody = nettyResponse.content();
    FileHttpTransmitter fileHttpTransmitter = new DefaultFileHttpTransmitter(nettyRequest, nettyResponse, channel);
    final EventHandler<ContextClose> closeEventHandler = new CloseEventHandler(channel);

    Response response = new DefaultResponse(responseStatus, responseHeaders, responseBody, fileHttpTransmitter, new Runnable() {
      @Override
      public void run() {
        nettyResponse.setStatus(responseStatus.getResponseStatus());
        responseHeaders.set(HttpHeaders.Names.CONTENT_LENGTH, responseBody.writerIndex());
        boolean shouldClose = true;
        if (channel.isOpen()) {
          if (isKeepAlive(nettyRequest)) {
            responseHeaders.set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            shouldClose = false;
          }
          ChannelFuture future = channel.writeAndFlush(nettyResponse);

          future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
              ContextClose eventContext = new ContextClose(new Date(), request, responseStatus);
              closeEventHandler.notify(eventContext);
            }
          });

          if (shouldClose) {
            future.addListener(ChannelFutureListener.CLOSE);
          }
        }
      }
    });

    InetSocketAddress socketAddress = (InetSocketAddress) channel.localAddress();
    BindAddress bindAddress = new InetSocketAddressBackedBindAddress(socketAddress);

    Context context = new DefaultContext(request, response, bindAddress, registry, ctx.executor(), blockingExecutorService, return404, closeEventHandler);
    handler.handle(context);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    if (!isIgnorableException(cause)) {
      cause.printStackTrace();
      if (ctx.channel().isActive()) {
        sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
      }
    }
  }

  private boolean isIgnorableException(Throwable throwable) {
    // There really does not seem to be a better way of detecting this kind of exception
    return throwable instanceof IOException && throwable.getMessage().equals("Connection reset by peer");
  }

  private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
    FullHttpResponse response = new DefaultFullHttpResponse(
      HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8));
    response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");

    // Close the connection as soon as the error message is sent.
    ctx.write(response).addListener(ChannelFutureListener.CLOSE);
  }
}
