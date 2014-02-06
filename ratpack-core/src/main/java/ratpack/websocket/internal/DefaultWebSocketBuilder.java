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

import ratpack.handling.Context;
import ratpack.launch.LaunchConfig;
import ratpack.func.Action;
import ratpack.func.Transformer;
import ratpack.websocket.WebSocket;
import ratpack.websocket.WebSocketBuilder;
import ratpack.websocket.WebSocketClose;
import ratpack.websocket.WebSocketMessage;

public class DefaultWebSocketBuilder<T> implements WebSocketBuilder<T> {

  private final Context context;
  private final Transformer<WebSocket, T> open;

  private String path = "/";
  private int maxLength;

  private Action<WebSocketMessage<T>> messageHandler = new Action<WebSocketMessage<T>>() {
    public void execute(WebSocketMessage<T> thing) throws Exception {
    }
  };

  private Action<WebSocketClose<T>> closeHandler = new Action<WebSocketClose<T>>() {
    public void execute(WebSocketClose<T> thing) throws Exception {
    }
  };

  public DefaultWebSocketBuilder(Context context, Transformer<WebSocket, T> open) {
    this.context = context;
    this.open = open;
    this.maxLength = context.get(LaunchConfig.class).getMaxContentLength();
  }

  @Override
  public WebSocketBuilder<T> path(String path) {
    this.path = path;
    return this;
  }

  @Override
  public WebSocketBuilder<T> onClose(Action<WebSocketClose<T>> action) {
    this.closeHandler = action;
    return this;
  }

  @Override
  public WebSocketBuilder<T> onMessage(Action<WebSocketMessage<T>> action) {
    messageHandler = action;
    return this;
  }

  @Override
  public WebSocketBuilder<T> maxLength(int maxLength) {
    return null;
  }

  @Override
  public void connect() {
    WebSocketConnector.connect(context, path, maxLength, new BuiltWebSocketHandler<>(open, closeHandler, messageHandler));
  }
}
