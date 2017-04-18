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

import io.netty.buffer.ByteBufAllocator;
import org.reactivestreams.Publisher;
import ratpack.func.Action;
import ratpack.handling.Context;
import ratpack.http.Response;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.render.Renderable;
import ratpack.sse.internal.DefaultEvent;
import ratpack.sse.internal.ServerSentEventEncoder;
import ratpack.stream.Streams;

/**
 * A {@link ratpack.handling.Context#render(Object) renderable} object for streaming server side events.
 * <p>
 * A {@link ratpack.render.Renderer renderer} for this type is implicitly provided by Ratpack core.
 * <p>
 * Example usage:
 * <pre class="java">{@code
 * import org.reactivestreams.Publisher;
 * import ratpack.http.client.ReceivedResponse;
 * import ratpack.sse.ServerSentEvents;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import java.time.Duration;
 * import java.util.Arrays;
 * import java.util.Objects;
 *
 * import static ratpack.sse.ServerSentEvents.serverSentEvents;
 * import static ratpack.stream.Streams.periodically;
 *
 * import static java.util.stream.Collectors.joining;
 *
 * import static org.junit.Assert.assertEquals;
 *
 * public class Example {
 *   public static void main(String[] args) throws Exception {
 *     EmbeddedApp.fromHandler(context -> {
 *       Publisher<String> stream = periodically(context, Duration.ofMillis(5), i ->
 *         i < 5 ? i.toString() : null
 *       );
 *
 *       ServerSentEvents events = serverSentEvents(stream, e ->
 *         e.id(Objects::toString).event("counter").data(i -> "event " + i)
 *       );
 *
 *       context.render(events);
 *     }).test(httpClient -> {
 *       ReceivedResponse response = httpClient.get();
 *       assertEquals("text/event-stream;charset=UTF-8", response.getHeaders().get("Content-Type"));
 *
 *       String expectedOutput = Arrays.asList(0, 1, 2, 3, 4)
 *         .stream()
 *         .map(i -> "id: " + i + "\nevent: counter\ndata: event " + i + "\n")
 *         .collect(joining("\n"))
 *         + "\n";
 *
 *       assertEquals(expectedOutput, response.getBody().getText());
 *     });
 *   }
 * }
 * }</pre>
 *
 * @see <a href="http://en.wikipedia.org/wiki/Server-sent_events" target="_blank">Wikipedia - Using server-sent events</a>
 * @see <a href="https://developer.mozilla.org/en-US/docs/Server-sent_events/Using_server-sent_events" target="_blank">MDN - Using server-sent events</a>
 */
public class ServerSentEvents implements Renderable {

  private final Publisher<? extends Event<?>> publisher;

  /**
   * Creates a new renderable object wrapping the event stream.
   * <p>
   * Takes a publisher of any type, and an action that mutates a created {@link Event} object for each stream item.
   * The action is executed for each item in the stream as it is emitted before being sent as a server sent event.
   * The state of the event object when the action completes will be used as the event.
   * <p>
   * The action <b>MUST</b> set one of {@code id}, {@code event}, {@code data} or {@code comment}.
   *
   * @param publisher the event stream
   * @param action the conversion of stream items to event objects
   * @param <T> the type of object in the event stream
   * @return a {@link ratpack.handling.Context#render(Object) renderable} object
   */
  public static <T> ServerSentEvents serverSentEvents(Publisher<T> publisher, Action<? super Event<T>> action) {
    return new ServerSentEvents(Streams.map(publisher, item -> {
      Event<T> event = action.with(new DefaultEvent<>(item));
      if (event.getData() == null && event.getId() == null && event.getEvent() == null && event.getComment() == null) {
        throw new IllegalArgumentException("You must supply at least one of data, event, id or comment");
      }
      return event;
    }));
  }

  private ServerSentEvents(Publisher<? extends Event<?>> publisher) {
    this.publisher = publisher;
  }

  /**
   * The stream of events.
   *
   * @return the stream of events
   */
  public Publisher<? extends Event<?>> getPublisher() {
    return publisher;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void render(Context context) throws Exception {
    ByteBufAllocator bufferAllocator = context.get(ByteBufAllocator.class);
    Response response = context.getResponse();
    response.getHeaders().add(HttpHeaderConstants.CONTENT_TYPE, HttpHeaderConstants.TEXT_EVENT_STREAM_CHARSET_UTF_8);
    response.getHeaders().add(HttpHeaderConstants.TRANSFER_ENCODING, HttpHeaderConstants.CHUNKED);
    response.getHeaders().add(HttpHeaderConstants.CACHE_CONTROL, HttpHeaderConstants.NO_CACHE_FULL);
    response.getHeaders().add(HttpHeaderConstants.PRAGMA, HttpHeaderConstants.NO_CACHE);
    response.sendStream(Streams.map(publisher, i -> ServerSentEventEncoder.INSTANCE.encode(i, bufferAllocator)));
  }

}
