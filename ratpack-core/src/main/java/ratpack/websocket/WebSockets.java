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

package ratpack.websocket;

import ratpack.handling.Context;
import ratpack.launch.LaunchConfig;
import ratpack.func.Transformer;
import ratpack.websocket.internal.DefaultWebSocketBuilder;
import ratpack.websocket.internal.WebSocketConnector;

public abstract class WebSockets {

  public static <T> WebSocketBuilder<T> websocket(Context context, Transformer<WebSocket, T> openAction) {
    return new DefaultWebSocketBuilder<>(context, openAction);
  }

  public static void websocket(Context context, WebSocketHandler<?> handler) {
    WebSocketConnector.connect(context, "/", context.get(LaunchConfig.class).getMaxContentLength(), handler);
  }

}
