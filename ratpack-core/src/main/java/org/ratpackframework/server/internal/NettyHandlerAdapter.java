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
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.ratpackframework.error.internal.DefaultClientErrorHandler;
import org.ratpackframework.error.internal.DefaultServerErrorHandler;
import org.ratpackframework.error.internal.ErrorCatchingHandler;
import org.ratpackframework.file.internal.ActivationBackedMimeTypes;
import org.ratpackframework.file.internal.DefaultFileSystemBinding;
import org.ratpackframework.handling.Context;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.handling.internal.ClientErrorHandler;
import org.ratpackframework.handling.internal.DefaultContext;
import org.ratpackframework.http.Request;
import org.ratpackframework.http.Response;
import org.ratpackframework.http.internal.DefaultRequest;
import org.ratpackframework.http.internal.DefaultResponse;
import org.ratpackframework.server.RatpackServerSettings;
import org.ratpackframework.registry.internal.RootRegistry;

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

@ChannelHandler.Sharable
public class NettyHandlerAdapter extends ChannelInboundHandlerAdapter {

  private final Handler handler;
  private final RootRegistry rootServiceRegistry;
  private final RatpackServerSettings settings;
  private Handler return404;

  public NettyHandlerAdapter(Handler handler, RatpackServerSettings settings) {
    this.settings = settings;
    this.handler = new ErrorCatchingHandler(handler);
    this.rootServiceRegistry = new RootRegistry(
      ImmutableList.of(
        settings,
        new DefaultServerErrorHandler(),
        new DefaultClientErrorHandler(),
        new ActivationBackedMimeTypes(),
        new DefaultFileSystemBinding(settings.getBaseDir())
      )
    );

    return404 = new ClientErrorHandler(NOT_FOUND.code());
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    FullHttpRequest nettyRequest = (FullHttpRequest) msg;

    if (!nettyRequest.getDecoderResult().isSuccess()) {
      sendError(ctx, HttpResponseStatus.BAD_REQUEST);
      return;
    }

    FullHttpResponse nettyResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

    Request request = new DefaultRequest(nettyRequest);

    HttpVersion version = nettyRequest.getProtocolVersion();
    boolean keepAlive = version == HttpVersion.HTTP_1_1
        || (version == HttpVersion.HTTP_1_0 && "Keep-Alive".equalsIgnoreCase(nettyRequest.headers().get("Connection")));

    Response response = new DefaultResponse(nettyResponse, ctx.channel(), keepAlive, version);
    final Context context = new DefaultContext(request, response, settings, ctx, rootServiceRegistry, return404);

    handler.handle(context);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    cause.printStackTrace();
    if (ctx.channel().isActive()) {
      sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
    FullHttpResponse response = new DefaultFullHttpResponse(
        HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8));
    response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");

    // Close the connection as soon as the error message is sent.
    ctx.write(response).addListener(ChannelFutureListener.CLOSE);
  }
}
