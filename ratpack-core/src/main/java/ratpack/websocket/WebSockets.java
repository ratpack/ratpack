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

import org.reactivestreams.Publisher;
import ratpack.func.Function;
import ratpack.handling.Context;
import ratpack.launch.LaunchConfig;
import ratpack.websocket.internal.DefaultWebSocketConnector;
import ratpack.websocket.internal.WebSocketEngine;
import ratpack.websocket.internal.WebsocketBroadcastSubscriber;

public abstract class WebSockets {

  public static <T> WebSocketConnector<T> websocket(Context context, Function<WebSocket, T> openAction) {
    return new DefaultWebSocketConnector<>(context, openAction);
  }

  public static void websocket(Context context, WebSocketHandler<?> handler) {
    WebSocketEngine.connect(context, "/", context.get(LaunchConfig.class).getMaxContentLength(), handler);
  }

  public static void websocketBroadcast(final Context context, final Publisher<String> broadcaster) {
    websocket(context, new AutoCloseWebSocketHandler<AutoCloseable>() {
      @Override
      public AutoCloseable onOpen(final WebSocket webSocket) throws Exception {
        WebsocketBroadcastSubscriber subscriber = new WebsocketBroadcastSubscriber(webSocket);
        context.stream(broadcaster).subscribe(subscriber);
        return subscriber;
      }
    });
  }
}
