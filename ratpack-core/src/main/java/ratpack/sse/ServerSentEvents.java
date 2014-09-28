/*
 * Copyright 2014 the original author or authors.
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

package ratpack.sse;

import org.reactivestreams.Publisher;

/**
 * A {@link ratpack.handling.Context#render(Object) renderable} object for streaming server side events.
 * <p>
 * A {@link ratpack.render.Renderer renderer} for this type is implicitly provided by Ratpack core.
 * <p>
 * Example usage:
 * <pre class="java">{@code
 * import org.reactivestreams.Publisher;
 * import ratpack.http.client.ReceivedResponse;
 * import ratpack.sse.ServerSentEvent;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import java.util.Arrays;
 *
 * import static java.util.concurrent.TimeUnit.MILLISECONDS;
 * import static java.util.stream.Collectors.joining;
 * import static ratpack.sse.ServerSentEvent.serverSentEvent;
 * import static ratpack.sse.ServerSentEvents.serverSentEvents;
 * import static ratpack.stream.Streams.periodically;
 *
 * public class Example {
 *   public static void main(String[] args) throws Exception {
 *     EmbeddedApp.fromHandler(context -> {
 *         Publisher<ServerSentEvent> stream = periodically(context.getLaunchConfig(), 5, MILLISECONDS,
 *           i -> i < 5
 *             ? serverSentEvent(s -> s.id(i.toString()).event("counter").data("event " + i))
 *             : null
 *         );
 *
 *         context.render(serverSentEvents(stream));
 *       }
 *     ).test(httpClient -> {
 *       ReceivedResponse response = httpClient.get();
 *       assert response.getHeaders().get("Content-Type").equals("text/event-stream;charset=UTF-8");
 *
 *       String expectedOutput = Arrays.asList(0, 1, 2, 3, 4)
 *         .stream()
 *         .map(i -> "event: counter\ndata: event " + i + "\nid: " + i + "\n")
 *         .collect(joining("\n"))
 *         + "\n";
 *
 *       assert response.getBody().getText().equals(expectedOutput);
 *     });
 *   }
 * }
 * }</pre>
 *
 * @see <a href="http://en.wikipedia.org/wiki/Server-sent_events" target="_blank">Wikipedia - Using server-sent events</a>
 * @see <a href="https://developer.mozilla.org/en-US/docs/Server-sent_events/Using_server-sent_events" target="_blank">MDN - Using server-sent events</a>
 */
public class ServerSentEvents {

  public static ServerSentEvents serverSentEvents(final Publisher<? extends ServerSentEvent> publisher) {
    return new ServerSentEvents(publisher);
  }

  private final Publisher<? extends ServerSentEvent> publisher;

  private ServerSentEvents(Publisher<? extends ServerSentEvent> publisher) {
    this.publisher = publisher;
  }

  public Publisher<? extends ServerSentEvent> getPublisher() {
    return publisher;
  }
}
