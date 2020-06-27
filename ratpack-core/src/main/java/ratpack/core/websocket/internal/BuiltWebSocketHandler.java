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

package ratpack.core.websocket.internal;

import ratpack.core.websocket.WebSocket;
import ratpack.core.websocket.WebSocketClose;
import ratpack.core.websocket.WebSocketHandler;
import ratpack.core.websocket.WebSocketMessage;
import ratpack.func.Action;
import ratpack.func.Function;

import static ratpack.func.Exceptions.uncheck;

public class BuiltWebSocketHandler<T> implements WebSocketHandler<T> {

  private final Function<? super WebSocket, T> open;
  private final Action<? super WebSocketClose<T>> close;
  private final Action<? super WebSocketMessage<T>> message;

  public BuiltWebSocketHandler(Function<? super WebSocket, T> open, Action<? super WebSocketClose<T>> close, Action<? super WebSocketMessage<T>> message) {
    this.open = open;
    this.close = close;
    this.message = message;
  }

  @Override
  public T onOpen(WebSocket webSocket) throws Exception {
    return open.apply(webSocket);
  }

  @Override
  public void onClose(WebSocketClose<T> close) {
    try {
      this.close.execute(close);
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  @Override
  public void onMessage(WebSocketMessage<T> frame) {
    try {
      this.message.execute(frame);
    } catch (Exception e) {
      throw uncheck(e);
    }
  }
}
