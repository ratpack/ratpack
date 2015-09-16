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

import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.handling.Context;
import ratpack.server.ServerConfig;
import ratpack.websocket.*;

public class DefaultWebSocketConnector<T> implements WebSocketConnector<T> {

  private final Context context;
  private final Function<WebSocket, T> open;

  private class Spec implements WebSocketSpec<T> {
    private Action<? super WebSocketMessage<T>> messageHandler = Action.noop();
    private Action<? super WebSocketClose<T>> closeHandler = Action.noop();

    private String path = "/";
    private int maxLength;

    private Spec(int maxLength) {
      this.maxLength = maxLength;
    }

    @Override
    public WebSocketSpec<T> path(String path) {
      this.path = path;
      return this;
    }

    @Override
    public WebSocketSpec<T> onClose(Action<WebSocketClose<T>> action) {
      this.closeHandler = action;
      return this;
    }

    @Override
    public WebSocketSpec<T> onMessage(Action<WebSocketMessage<T>> action) {
      messageHandler = action;
      return this;
    }

    @Override
    public WebSocketSpec<T> maxLength(int maxLength) {
      this.maxLength = maxLength;
      return this;
    }
  }

  public DefaultWebSocketConnector(Context context, Function<WebSocket, T> open) {
    this.context = context;
    this.open = open;
  }

  @Override
  public void connect(Action<? super WebSocketSpec<T>> specAction) throws Exception {
    Spec spec = new Spec(context.get(ServerConfig.class).getMaxContentLength());
    specAction.execute(spec);
    WebSocketEngine.connect(context, spec.path, spec.maxLength, new BuiltWebSocketHandler<>(open, spec.closeHandler, spec.messageHandler));

  }

}
