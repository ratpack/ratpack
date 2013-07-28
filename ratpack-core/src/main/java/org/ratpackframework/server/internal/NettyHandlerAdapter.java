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
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.ratpackframework.server.RatpackServerSettings;
import org.ratpackframework.service.internal.RootServiceRegistry;
import org.ratpackframework.error.internal.DefaultClientErrorHandler;
import org.ratpackframework.error.internal.DefaultServerErrorHandler;
import org.ratpackframework.error.internal.ErrorCatchingHandler;
import org.ratpackframework.file.internal.ActivationBackedMimeTypes;
import org.ratpackframework.file.internal.DefaultFileSystemBinding;
import org.ratpackframework.handling.Exchange;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.handling.internal.ClientErrorHandler;
import org.ratpackframework.handling.internal.DefaultExchange;
import org.ratpackframework.http.Request;
import org.ratpackframework.http.Response;
import org.ratpackframework.http.internal.DefaultRequest;
import org.ratpackframework.http.internal.DefaultResponse;

import java.io.File;

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

@ChannelHandler.Sharable
public class NettyHandlerAdapter extends ChannelInboundMessageHandlerAdapter<FullHttpRequest> {

  private final Handler handler;
  private final RootServiceRegistry rootServiceRegistry;
  private final RatpackServerSettings settings;
  private Handler return404;

  public NettyHandlerAdapter(Handler handler, File baseDir, RatpackServerSettings settings) {
    this.settings = settings;
    this.handler = new ErrorCatchingHandler(handler);
    this.rootServiceRegistry = new RootServiceRegistry(
      ImmutableList.of(
        new DefaultServerErrorHandler(),
        new DefaultClientErrorHandler(),
        new ActivationBackedMimeTypes(),
        new DefaultFileSystemBinding(baseDir)
      )
    );

    return404 = new ClientErrorHandler(NOT_FOUND.code());
  }

  public void messageReceived(ChannelHandlerContext ctx, FullHttpRequest nettyRequest) throws Exception {
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
    final Exchange exchange = new DefaultExchange(request, response, settings, ctx, rootServiceRegistry, return404);

    handler.handle(exchange);
  }

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
