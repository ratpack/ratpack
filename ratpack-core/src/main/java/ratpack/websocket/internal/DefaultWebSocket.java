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
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import ratpack.util.Action;
import ratpack.websocket.WebSocket;
import ratpack.websocket.WebSocketClose;

import static ratpack.util.ExceptionUtils.uncheck;

public class DefaultWebSocket implements WebSocket {

  private final Channel channel;
  private final Action<? super WebSocketClose> closeHandler;

  public DefaultWebSocket(Channel channel, Action<? super WebSocketClose> closeHandler) {
    this.channel = channel;
    this.closeHandler = closeHandler;
  }

  @Override
  public void close() {
    channel.writeAndFlush(new CloseWebSocketFrame());
    channel.close().addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        try {
          closeHandler.execute(new DefaultWebSocketClose(false));
        } catch (Exception e) {
          throw uncheck(e);
        }
      }
    });
  }

  @Override
  public void send(String text) {
    channel.writeAndFlush(new TextWebSocketFrame(text));
  }

  @Override
  public void send(byte[] bytes) {

  }
}
