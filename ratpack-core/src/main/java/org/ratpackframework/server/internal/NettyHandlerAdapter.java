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

package org.ratpackframework.server.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListeningExecutorService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.ratpackframework.error.internal.DefaultClientErrorHandler;
import org.ratpackframework.error.internal.DefaultServerErrorHandler;
import org.ratpackframework.error.internal.ErrorCatchingHandler;
import org.ratpackframework.file.internal.*;
import org.ratpackframework.handling.Context;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.handling.internal.ClientErrorHandler;
import org.ratpackframework.handling.internal.DefaultContext;
import org.ratpackframework.http.MutableHeaders;
import org.ratpackframework.http.Request;
import org.ratpackframework.http.Response;
import org.ratpackframework.http.internal.*;
import org.ratpackframework.launch.LaunchConfig;
import org.ratpackframework.redirect.internal.DefaultRedirector;
import org.ratpackframework.registry.Registry;
import org.ratpackframework.registry.internal.RootRegistry;
import org.ratpackframework.render.Renderer;
import org.ratpackframework.server.BindAddress;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.ratpackframework.file.internal.DefaultIndexFiles.indexFiles;
import static org.ratpackframework.render.controller.RenderControllers.renderController;

@ChannelHandler.Sharable
public class NettyHandlerAdapter extends SimpleChannelInboundHandler<FullHttpRequest> {

  private final Handler handler;
  private final Handler return404;
  private final LaunchConfig launchConfig;
  private final ListeningExecutorService blockingExecutorService;

  private Registry<Object> registry;
  private final Lock registryLock = new ReentrantLock();

  public NettyHandlerAdapter(Handler handler, LaunchConfig launchConfig, ListeningExecutorService blockingExecutorService) {
    this.launchConfig = launchConfig;
    this.blockingExecutorService = blockingExecutorService;
    this.handler = new ErrorCatchingHandler(handler);
    this.return404 = new ClientErrorHandler(NOT_FOUND.code());
  }

  public void channelRead0(final ChannelHandlerContext ctx, FullHttpRequest nettyRequest) throws Exception {
    if (!nettyRequest.getDecoderResult().isSuccess()) {
      sendError(ctx, HttpResponseStatus.BAD_REQUEST);
      return;
    }

    final FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

    Request request = new DefaultRequest(new NettyHeadersBackedHeaders(nettyRequest.headers()), nettyRequest.getMethod().name(), nettyRequest.getUri(), nettyRequest.content());

    final HttpVersion version = nettyRequest.getProtocolVersion();
    final boolean keepAlive = version == HttpVersion.HTTP_1_1
      || (version == HttpVersion.HTTP_1_0 && "Keep-Alive".equalsIgnoreCase(nettyRequest.headers().get("Connection")));

    final Channel channel = ctx.channel();

    final DefaultStatus responseStatus = new DefaultStatus();
    final MutableHeaders responseHeaders = new NettyHeadersBackedMutableHeaders(nettyResponse.headers());
    final ByteBuf responseBody = nettyResponse.content();
    FileHttpTransmitter fileHttpTransmitter = new DefaultFileHttpTransmitter(nettyRequest, nettyResponse, channel);

    Response response = new DefaultResponse(responseStatus, responseHeaders, responseBody, fileHttpTransmitter, new Runnable() {
      @Override
      public void run() {
        nettyResponse.setStatus(responseStatus.getResponseStatus());
        responseHeaders.set(HttpHeaders.Names.CONTENT_LENGTH, responseBody.writerIndex());
        boolean shouldClose = true;
        if (channel.isOpen()) {
          if (keepAlive) {
            if (version == HttpVersion.HTTP_1_0) {
              responseHeaders.set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
            }
            shouldClose = false;
          }
          ChannelFuture future = channel.writeAndFlush(nettyResponse);
          if (shouldClose) {
            future.addListener(ChannelFutureListener.CLOSE);
          }
        }
      }
    });

    if (registry == null) {
      try {
        registryLock.lock();
        if (registry == null) {
          registry = createRegistry(channel);
        }
      } finally {
        registryLock.unlock();
      }
    }

    final Context context = new DefaultContext(request, response, registry, ctx.executor(), blockingExecutorService, return404);

    handler.handle(context);
  }

  private Registry<Object> createRegistry(Channel channel) {
    InetSocketAddress socketAddress = (InetSocketAddress) channel.localAddress();

    BindAddress bindAddress = new InetSocketAddressBackedBindAddress(socketAddress);

    ImmutableList<Renderer<?>> renderers = new ImmutableList.Builder<Renderer<?>>()
      .add(new FileRenderer())
      .build();

    // If you update this list, update the class level javadoc on Context.
    return new RootRegistry<>(
      ImmutableList.of(
        new DefaultFileSystemBinding(launchConfig.getBaseDir()),
        new ActivationBackedMimeTypes(),
        bindAddress,
        new DefaultPublicAddress(launchConfig.getPublicAddress(), bindAddress),
        new DefaultRedirector(),
        new DefaultClientErrorHandler(),
        new DefaultServerErrorHandler(),
        launchConfig,
        renderController(renderers),
        indexFiles(launchConfig)
      )
    );
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
