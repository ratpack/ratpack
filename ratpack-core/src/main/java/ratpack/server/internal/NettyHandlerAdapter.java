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
import ratpack.event.internal.DefaultEventController;
import ratpack.event.internal.EventController;
import ratpack.file.FileRenderer;
import ratpack.file.FileSystemBinding;
import ratpack.file.MimeTypes;
import ratpack.file.internal.*;
import ratpack.form.FormParser;
import ratpack.form.internal.MultipartFormParser;
import ratpack.form.internal.UrlEncodedFormParser;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.handling.Redirector;
import ratpack.handling.RequestOutcome;
import ratpack.handling.direct.DirectChannelAccess;
import ratpack.handling.direct.internal.DefaultDirectChannelAccess;
import ratpack.handling.internal.ClientErrorForwardingHandler;
import ratpack.handling.internal.DefaultContext;
import ratpack.handling.internal.DefaultRedirector;
import ratpack.handling.internal.DefaultRequestOutcome;
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
import ratpack.server.Stopper;
import ratpack.util.Action;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

@ChannelHandler.Sharable
public class NettyHandlerAdapter extends SimpleChannelInboundHandler<FullHttpRequest> {

  private final Handler[] handlers;
  private final Handler return404;
  private final ListeningExecutorService backgroundExecutorService;

  private final ConcurrentHashMap<Channel, Action<Object>> channelSubscriptions = new ConcurrentHashMap<>(0);

  private Registry registry;

  public NettyHandlerAdapter(Stopper stopper, Handler handler, LaunchConfig launchConfig, ListeningExecutorService backgroundExecutorService) {
    this.backgroundExecutorService = backgroundExecutorService;
    this.handlers = new Handler[]{new ErrorCatchingHandler(handler)};
    this.return404 = new ClientErrorForwardingHandler(NOT_FOUND.code());

    this.registry = RegistryBuilder.builder()
      // If you update this list, update the class level javadoc on Context.
      .add(Stopper.class, stopper)
      .add(FileSystemBinding.class, new DefaultFileSystemBinding(launchConfig.getBaseDir()))
      .add(MimeTypes.class, new ActivationBackedMimeTypes())
      .add(PublicAddress.class, new DefaultPublicAddress(launchConfig.getPublicAddress(), launchConfig.getSSLContext() == null ? "http" : "https"))
      .add(Redirector.class, new DefaultRedirector())
      .add(ClientErrorHandler.class, new DefaultClientErrorHandler())
      .add(ServerErrorHandler.class, new DefaultServerErrorHandler())
      .add(LaunchConfig.class, launchConfig)
      .add(FileRenderer.class, new DefaultFileRenderer())
      .add(CharSequenceRenderer.class, new DefaultCharSequenceRenderer())
      .add(FormParser.class, new UrlEncodedFormParser())
      .add(FormParser.class, new MultipartFormParser())
      .build();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (!(msg instanceof FullHttpRequest)) {
      Action<Object> subscriber = channelSubscriptions.get(ctx.channel());
      if (subscriber != null) {
        subscriber.execute(msg);
        return;
      }
    }
    super.channelRead(ctx, msg);
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

    final EventController<RequestOutcome> requestOutcomeEventController = new DefaultEventController<>();

    // We own the lifecycle
    nettyRequest.content().retain();

    final Response response = new DefaultResponse(responseStatus, responseHeaders, responseBody, fileHttpTransmitter, new Action<Response>() {
      @Override
      public void execute(final Response response) {
        nettyRequest.content().release();
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
              RequestOutcome requestOutcome = new DefaultRequestOutcome(request, response, System.currentTimeMillis());
              requestOutcomeEventController.fire(requestOutcome);
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

    Action<Action<Object>> subscribeHandler = new Action<Action<Object>>() {
      @Override
      public void execute(Action<Object> thing) throws Exception {
        channelSubscriptions.put(channel, thing);
        channel.closeFuture().addListener(new ChannelFutureListener() {
          @Override
          public void operationComplete(ChannelFuture future) throws Exception {
            channelSubscriptions.remove(channel);
          }
        });
      }
    };

    DirectChannelAccess directChannelAccess = new DefaultDirectChannelAccess(channel, subscribeHandler);

    ScheduledExecutorService foregroundExecutorService = ctx.executor();
    Context context = new DefaultContext(directChannelAccess, request, response, bindAddress, registry, backgroundExecutorService, foregroundExecutorService, requestOutcomeEventController.getRegistry(), handlers, 0, return404);
    context.next();
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
