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

package ratpack.websocket.internal;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.*;
import ratpack.handling.Context;
import ratpack.handling.direct.DirectChannelAccess;
import ratpack.http.Request;
import ratpack.launch.LaunchConfig;
import ratpack.server.PublicAddress;
import ratpack.util.Action;
import ratpack.websocket.WebSocket;
import ratpack.websocket.WebSocketBuilder;
import ratpack.websocket.WebSocketClose;
import ratpack.websocket.WebSocketFrame;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.netty.handler.codec.http.HttpHeaders.Names.SEC_WEBSOCKET_KEY;
import static io.netty.handler.codec.http.HttpHeaders.Names.SEC_WEBSOCKET_VERSION;
import static io.netty.handler.codec.http.HttpMethod.valueOf;
import static ratpack.util.ExceptionUtils.toException;
import static ratpack.util.ExceptionUtils.uncheck;

public class DefaultWebSocketBuilder implements WebSocketBuilder {

  private final Context context;

  private String path = "/";
  private int maxLength;
  private Action<? super WebSocketClose> closeHandler;
  private Action<? super WebSocketFrame> messageHandler;
  private Action<? super WebSocket> handler;

  public DefaultWebSocketBuilder(Context context) {
    this.context = context;
    this.maxLength = context.get(LaunchConfig.class).getMaxContentLength();
  }

  @Override
  public WebSocketBuilder path(String path) {
    this.path = path;
    return this;
  }

  @Override
  public WebSocketBuilder onClose(Action<? super WebSocketClose> action) {
    this.closeHandler = action;
    return this;
  }

  @Override
  public WebSocketBuilder onMessage(Action<? super WebSocketFrame> action) {
    messageHandler = action;
    return this;
  }

  @Override
  public WebSocketBuilder maxLength(int maxLength) {
    return null;
  }

  @Override
  public void connect(Action<? super WebSocket> action) {
    this.handler = action;

    PublicAddress publicAddress = context.get(PublicAddress.class);
    URI address = publicAddress.getAddress(context);
    URI httpPath = address.resolve(path);

    URI wsPath;
    try {
      wsPath = new URI("ws", httpPath.getUserInfo(), httpPath.getHost(), httpPath.getPort(), httpPath.getPath(), httpPath.getQuery(), httpPath.getFragment());
    } catch (URISyntaxException e) {
      throw uncheck(e);
    }

    WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(wsPath.toString(), null, false, maxLength);

    Request request = context.getRequest();
    HttpMethod method = valueOf(request.getMethod().getName());
    FullHttpRequest nettyRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, request.getUri());
    nettyRequest.headers().add(SEC_WEBSOCKET_VERSION, request.getHeaders().get(SEC_WEBSOCKET_VERSION));
    nettyRequest.headers().add(SEC_WEBSOCKET_KEY, request.getHeaders().get(SEC_WEBSOCKET_KEY));

    final WebSocketServerHandshaker handshaker = factory.newHandshaker(nettyRequest);

    final DirectChannelAccess directChannelAccess = context.getDirectChannelAccess();
    final Channel channel = directChannelAccess.getChannel();

    handshaker.handshake(channel, nettyRequest).addListener(new ChannelFutureListener() {
      public void operationComplete(ChannelFuture future) throws Exception {
        if (future.isSuccess()) {
          final AtomicBoolean open = new AtomicBoolean(true);
          final WebSocket webSocket = new DefaultWebSocket(channel, closeHandler, open);

          directChannelAccess.takeOwnership(new Action<Object>() {
            @Override
            public void execute(Object msg) throws Exception {
              if (msg instanceof io.netty.handler.codec.http.websocketx.WebSocketFrame) {
                io.netty.handler.codec.http.websocketx.WebSocketFrame frame = (io.netty.handler.codec.http.websocketx.WebSocketFrame) msg;
                if (frame instanceof CloseWebSocketFrame) {
                  open.set(false);
                  handshaker.close(channel, (CloseWebSocketFrame) frame.retain()).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                      closeHandler.execute(new DefaultWebSocketClose(true));
                    }
                  });
                  return;
                }
                if (frame instanceof PingWebSocketFrame) {
                  channel.write(new PongWebSocketFrame(frame.content().retain()));
                  return;
                }
                if (frame instanceof TextWebSocketFrame) {
                  messageHandler.execute(new DefaultWebSocketFrame(webSocket, ((TextWebSocketFrame) frame).text()));
                }
              }
            }

          });

          handler.execute(webSocket);
        } else {
          context.error(toException(future.cause()));
        }
      }
    });
  }
}
