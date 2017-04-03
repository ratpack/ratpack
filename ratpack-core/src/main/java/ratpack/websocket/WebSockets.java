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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.CharsetUtil;
import org.reactivestreams.Publisher;
import ratpack.func.Function;
import ratpack.handling.Context;
import ratpack.server.ServerConfig;
import ratpack.stream.Streams;
import ratpack.websocket.internal.DefaultWebSocketConnector;
import ratpack.websocket.internal.WebSocketEngine;
import ratpack.websocket.internal.WebsocketBroadcastSubscriber;

import java.nio.CharBuffer;

/**
 * WebSockets support for Ratpack.
 * <p>
 * An example that broadcasts strings to a websocket every second:
 * <pre class="java-chain-dsl">{@code
 * import org.reactivestreams.Publisher;
 * import ratpack.test.embed.EmbeddedApp;
 * import ratpack.websocket.WebSockets;
 *
 * import java.time.Duration;
 *
 * import static ratpack.stream.Streams.periodically;
 *
 * chain.get("whatever", context -> {
 *   Publisher<String> stream = periodically(context, Duration.ofSeconds(1), i ->
 *     i < 5 ? i.toString() : null
 *   );
 *
 *   WebSockets.websocketBroadcast(context, stream);
 * });
 * }</pre>
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/WebSockets/Writing_WebSocket_client_applications">Writing WebSocket client applications</a>
 */
public abstract class WebSockets {

  public static <T> WebSocketConnector<T> websocket(Context context, Function<WebSocket, T> openAction) {
    return new DefaultWebSocketConnector<>(context, openAction);
  }

  public static void websocket(Context context, WebSocketHandler<?> handler) {
    WebSocketEngine.connect(context, "/", context.get(ServerConfig.class).getMaxContentLength(), handler);
  }

  /**
   * Sets up a websocket that sends the published Strings to a client.
   * <p>
   * This takes the place of a {@link Streams#bindExec(Publisher)} call.
   *
   * @param context the request handling context
   * @param broadcaster a {@link Publisher} of Strings to send to the websocket client
   */
  public static void websocketBroadcast(final Context context, final Publisher<String> broadcaster) {
    ByteBufAllocator bufferAllocator = context.get(ByteBufAllocator.class);
    websocketByteBufBroadcast(context, Streams.map(broadcaster, s ->
        ByteBufUtil.encodeString(bufferAllocator, CharBuffer.wrap(s), CharsetUtil.UTF_8)
    ));
  }

  /**
   * Sets up a websocket that sends the published byte buffers to a client.
   * <p>
   * This takes the place of a {@link Streams#bindExec(Publisher)} call.
   *
   * @param context the request handling context
   * @param broadcaster a {@link Publisher} of {@link ByteBuf}s to send to the websocket client
   */
  public static void websocketByteBufBroadcast(final Context context, final Publisher<ByteBuf> broadcaster) {
    websocket(context, new AutoCloseWebSocketHandler<AutoCloseable>() {
      @Override
      public AutoCloseable onOpen(final WebSocket webSocket) throws Exception {
        WebsocketBroadcastSubscriber subscriber = new WebsocketBroadcastSubscriber(webSocket);
        Streams.bindExec(broadcaster, ByteBuf::release).subscribe(subscriber);
        return subscriber;
      }
    });
  }
}
