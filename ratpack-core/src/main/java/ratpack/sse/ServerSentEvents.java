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
import ratpack.func.Function;
import ratpack.handling.Context;
import ratpack.http.Response;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.render.Renderable;
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
public class ServerSentEvents implements Renderable {

  private final Publisher<? extends Event<?>> publisher;

  /**
   * Creates a new renderable object wrapping the event stream.
   * <p>
   * Takes a publisher of any type, and an action that mutates a created {@link Event} object for each stream item.
   * The action is executed for each item in the stream as it is emitted before being sent as a server sent event.
   * The state of the event object when the action completes will be used as the event.
   * <p>
   * The action <b>MUST</b> set one of the {@code id}, {@code event}, {@code data}.
   *
   * @param publisher the event stream
   * @param action the conversion of stream items to event objects
   * @param <T> the type of object in the event stream
   * @return a {@link ratpack.handling.Context#render(Object) renderable} object
   */
  public static <T> ServerSentEvents serverSentEvents(Publisher<T> publisher, Action<? super Event<T>> action) {
    return new ServerSentEvents(Streams.map(publisher, item -> {
      EventImpl<T> event = action.with(new EventImpl<>(item));
      if (event.id == null && event.event == null && event.data == null) {
        throw new IllegalArgumentException("You must supply at least one of data, event, id");
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
    response.getHeaders().add(HttpHeaderConstants.CACHE_CONTROL, HttpHeaderConstants.NO_CACHE_FULL);
    response.getHeaders().add(HttpHeaderConstants.PRAGMA, HttpHeaderConstants.NO_CACHE);
    response.sendStream(Streams.map(publisher, i -> ServerSentEventEncoder.INSTANCE.encode(i, bufferAllocator)));
  }

  /**
   * An individual event in a server sent event stream.
   * <p>
   * The {@link #getItem() item} is the item in the data stream being emitted as a server sent event.
   * It can be used to derive values for the {@link #id}, {@link #event} and/or {@link #data} fields.
   * <p>
   * By default, the {@code id}, {@code event} and {@code data} fields are set to {@code null}.
   *
   * @see ratpack.sse.ServerSentEvents#serverSentEvents
   */
  public static interface Event<T> {

    /**
     * The stream item that this event
     *
     * @return a supporting object
     */
    T getItem();

    /**
     * The “id” value of the event.
     * <p>
     * {@code null} by default.
     *
     * @return the “id” value of the event
     */
    String getId();

    /**
     * The “event” value of the event.
     * <p>
     * {@code null} by default.
     *
     * @return the “event” value of the event
     */
    String getEvent();

    /**
     * The “data” value of the event.
     * <p>
     * {@code null} by default.
     *
     * @return the “data” value of the event
     */
    String getData();

    /**
     * Sets the “id” value of the event to the return value of the given function.
     * <p>
     * The function receives the {@link #getItem() item} and is executed immediately.
     *
     * @param function a generator for the “id” value of the event
     * @return this
     * @throws Exception any thrown by {@code function}
     */
    Event<T> id(Function<? super T, String> function) throws Exception;

    /**
     * Specify the event id for the server sent event.
     *
     * @param id the event id
     * @return this
     */
    Event<T> id(String id);

    /**
     * Sets the “event” value of the event to the return value of the given function.
     * <p>
     * The function receives the {@link #getItem() item} and is executed immediately.
     *
     * @param function a generator for the “event” value of the event
     * @return this
     * @throws Exception any thrown by {@code function}
     */
    Event<T> event(Function<? super T, String> function) throws Exception;

    /**
     * Specify the event type for the server sent event.
     *
     * @param event the event type
     * @return this
     */
    Event<T> event(String event);

    /**
     * Sets the “data” value of the event to the return value of the given function.
     * <p>
     * The function receives the {@link #getItem() item} and is executed immediately.
     *
     * @param function a generator for the “data” value of the event
     * @return this
     * @throws Exception any thrown by {@code function}
     */
    Event<T> data(Function<? super T, String> function) throws Exception;

    /**
     * Specify the event data for the server sent event.
     *
     * @param data the event data
     * @return this
     */
    Event<T> data(String data);

  }

  private static class EventImpl<T> implements Event<T> {

    private final T item;

    private String id;
    private String event;
    private String data;

    public EventImpl(T item) {
      this.item = item;
    }

    @Override
    public T getItem() {
      return item;
    }

    @Override
    public Event<T> id(Function<? super T, String> id) throws Exception {
      id(id.apply(item));
      return this;
    }

    @Override
    public Event<T> id(String id) {
      this.id = id;
      return this;
    }

    @Override
    public Event<T> event(Function<? super T, String> id) throws Exception {
      event(id.apply(item));
      return this;
    }

    @Override
    public Event<T> event(String event) {
      this.event = event;
      return this;
    }

    @Override
    public Event<T> data(Function<? super T, String> id) throws Exception {
      data(id.apply(item));
      return this;
    }

    @Override
    public Event<T> data(String data) {
      this.data = data;
      return this;
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public String getEvent() {
      return event;
    }

    @Override
    public String getData() {
      return data;
    }
  }

}
