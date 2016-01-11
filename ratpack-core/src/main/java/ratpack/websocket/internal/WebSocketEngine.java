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
import ratpack.server.PublicAddress;
import ratpack.websocket.WebSocket;
import ratpack.websocket.WebSocketHandler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.netty.handler.codec.http.HttpHeaderNames.SEC_WEBSOCKET_KEY;
import static io.netty.handler.codec.http.HttpHeaderNames.SEC_WEBSOCKET_VERSION;
import static io.netty.handler.codec.http.HttpMethod.valueOf;
import static ratpack.util.Exceptions.toException;
import static ratpack.util.Exceptions.uncheck;

public class WebSocketEngine {

  @SuppressWarnings("deprecation")
  public static <T> void connect(final Context context, String path, int maxLength, final WebSocketHandler<T> handler) {
    PublicAddress publicAddress = context.get(PublicAddress.class);
    URI address = publicAddress.get(context);
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
    if (!channel.config().isAutoRead()) {
      channel.config().setAutoRead(true);
    }

    handshaker.handshake(channel, nettyRequest).addListener(new HandshakeFutureListener<>(context, handshaker, handler));
  }

  private static class HandshakeFutureListener<T> implements ChannelFutureListener {

    private final Context context;
    private final WebSocketServerHandshaker handshaker;
    private final WebSocketHandler<T> handler;

    private volatile T openResult;
    private final CountDownLatch openLatch = new CountDownLatch(1);

    public HandshakeFutureListener(Context context, WebSocketServerHandshaker handshaker, WebSocketHandler<T> handler) {
      this.context = context;
      this.handshaker = handshaker;
      this.handler = handler;
    }

    public void operationComplete(ChannelFuture future) throws Exception {
      if (future.isSuccess()) {
        final AtomicBoolean open = new AtomicBoolean(true);
        final WebSocket webSocket = new DefaultWebSocket(context.getDirectChannelAccess().getChannel(), open, () -> {
          try {
            handler.onClose(new DefaultWebSocketClose<>(false, openResult));
          } catch (Exception e) {
            throw uncheck(e);
          }
        });

        final DirectChannelAccess directAccessChannel = context.getDirectChannelAccess();
        final Channel channel = directAccessChannel.getChannel();

        channel.closeFuture().addListener(fu -> {
            try {
              handler.onClose(new DefaultWebSocketClose<>(true, openResult));
            } catch (Exception e) {
              throw uncheck(e);
            }
          });

        directAccessChannel.takeOwnership(msg -> {
          openLatch.await();
          if (channel.isOpen()) {
            if (msg instanceof WebSocketFrame) {
              WebSocketFrame frame = (WebSocketFrame) msg;
              if (frame instanceof CloseWebSocketFrame) {
                open.set(false);
                handshaker.close(channel, (CloseWebSocketFrame) frame).addListener(future1 -> handler.onClose(new DefaultWebSocketClose<>(true, openResult)));
                return;
              }
              if (frame instanceof PingWebSocketFrame) {
                channel.write(new PongWebSocketFrame(frame.content()));
                return;
              }
              if (frame instanceof TextWebSocketFrame) {
                TextWebSocketFrame textWebSocketFrame = (TextWebSocketFrame) frame;
                handler.onMessage(new DefaultWebSocketMessage<>(webSocket, textWebSocketFrame.text(), openResult));
                frame.release();
              }
            }
          }
        });

        try {
          openResult = handler.onOpen(webSocket);
        } catch (Exception e) {
          handshaker.close(context.getDirectChannelAccess().getChannel(), new CloseWebSocketFrame(1011, e.getMessage()));
        }
        openLatch.countDown();
      } else {
        context.error(toException(future.cause()));
      }
    }
  }
}
