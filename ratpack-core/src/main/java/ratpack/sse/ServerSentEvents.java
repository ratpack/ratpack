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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.EventLoop;
import io.netty.util.ReferenceCountUtil;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import ratpack.api.Nullable;
import ratpack.exec.Execution;
import ratpack.exec.internal.DefaultExecution;
import ratpack.func.Action;
import ratpack.handling.Context;
import ratpack.http.Response;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.render.Renderable;
import ratpack.sse.internal.DefaultEvent;
import ratpack.sse.internal.ServerSentEventEncoder;
import ratpack.sse.internal.ServerSentEventStreamBufferSettings;
import ratpack.stream.Streams;
import ratpack.stream.bytebuf.internal.ByteBufBufferingSubscription;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static java.util.Objects.requireNonNull;

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
 * import ratpack.sse.ServerSentEvents;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import java.time.Duration;
 * import java.util.Arrays;
 * import java.util.Objects;
 *
 * import static ratpack.stream.Streams.periodically;
 *
 * import static java.util.stream.Collectors.joining;
 *
 * import static org.junit.Assert.assertEquals;
 *
 * public class Example {
 *   public static void main(String[] args) throws Exception {
 *     EmbeddedApp.fromHandler(context -> {
 *       Publisher<ServerSentEvent> stream = periodically(context, Duration.ofMillis(5), i ->
 *         i < 5 ? ServerSentEvent.builder().id(i.toString()).event("counter").data("event " + i).build() : null
 *       );
 *
 *       context.render(ServerSentEvents.builder().build(stream));
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
 * @see #builder()
 */
public class ServerSentEvents implements Renderable {

  private static final ByteBuf HEARTBEAT = Unpooled.unreleasableBuffer(
      Unpooled.wrappedBuffer(": keepalive heartbeat\n\n".getBytes(StandardCharsets.UTF_8))
  );
  private final Publisher<? extends ServerSentEvent> publisher;

  private final boolean noContentOnEmpty;
  @Nullable
  private final Duration heartbeatFrequency;
  @Nullable
  private final ServerSentEventStreamBufferSettings bufferSettings;

  /**
   * Creates a builder for an event stream.
   *
   * @return a builder for an event stream
   */
  public static ServerSentEventsBuilder builder() {
    return new BuilderImpl();
  }

  /**
   * Deprecated.
   *
   * @deprecated since 1.10 - use {@link #builder()}
   */
  @Deprecated
  public static <T> ServerSentEvents serverSentEvents(Publisher<T> publisher, Action<? super Event<T>> action) {
    Publisher<ServerSentEvent> eventPublisher = DefaultEvent.toEvents(publisher, action);
    return new ServerSentEvents(eventPublisher, false, null, null);
  }

  private ServerSentEvents(Publisher<? extends ServerSentEvent> publisher, boolean noContentOnEmpty, @Nullable Duration heartbeatFrequency, @Nullable ServerSentEventStreamBufferSettings bufferSettings) {
    this.publisher = publisher;
    this.noContentOnEmpty = noContentOnEmpty;
    this.heartbeatFrequency = heartbeatFrequency;
    this.bufferSettings = bufferSettings;
  }

  /**
   * Deprecated.
   *
   * @deprecated since 1.10 with no replacement
   */
  @Deprecated
  @Nullable
  public Publisher<? extends Event<?>> getPublisher() {
    return Streams.map(publisher, DefaultEvent::fromServerSentEvent);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void render(Context context) throws Exception {
    Response response = context.getResponse();
    response.getHeaders().add(HttpHeaderConstants.CACHE_CONTROL, HttpHeaderConstants.NO_CACHE_FULL);
    response.getHeaders().add(HttpHeaderConstants.PRAGMA, HttpHeaderConstants.NO_CACHE);

    if (noContentOnEmpty) {
      renderWithNoContentOnEmpty(context);
    } else {
      renderStream(context, publisher);
    }
  }

  private void renderWithNoContentOnEmpty(Context context) {
    // Subscribe so we can listen for the first event
    DefaultExecution execution = DefaultExecution.require();
    execution.delimit(context::error, continuation ->
        Execution.fork().eventLoop(execution.getEventLoop()).start(e -> publisher.subscribe(new Subscriber<ServerSentEvent>() {

          private Subscription subscription;
          private Subscriber<? super ServerSentEvent> subscriber;

          @Override
          public void onSubscribe(Subscription s) {
            subscription = s;
            subscription.request(1);
          }

          @Override
          public void onNext(ServerSentEvent event) {
            if (subscriber == null) {
              // This is the first event, we need to set up the forward to the response.

              // A publisher for the item we have consumed.
              Publisher<ServerSentEvent> consumedPublisher = Streams.publish(Collections.singleton(event));

              // A publisher that will forward what we haven't consumed.
              Publisher<ServerSentEvent> restPublisher = s -> {
                // Upstream signals will flow through us, and we need to forward to this subscriber
                subscriber = s;

                // Pass through our subscription so that the new subscriber controls demand.
                s.onSubscribe(requireNonNull(subscription));
              };

              // Join them together so that we send the whole thing.
              continuation.resume(() -> renderStream(context, Streams.concat(Arrays.asList(consumedPublisher, restPublisher))));
            } else {
              subscriber.onNext(event);
            }
          }

          @Override
          public void onError(Throwable t) {
            if (subscriber == null) {
              continuation.resume(() -> context.error(t));
            } else {
              subscriber.onError(t);
            }
          }

          @Override
          public void onComplete() {
            if (subscriber == null) {
              continuation.resume(() -> emptyStream(context));
            } else {
              subscriber.onComplete();
            }
          }
        }))
    );
  }

  private void renderStream(Context context, Publisher<? extends ServerSentEvent> events) {
    Response response = context.getResponse();
    response.getHeaders().add(HttpHeaderConstants.CONTENT_TYPE, HttpHeaderConstants.TEXT_EVENT_STREAM_CHARSET_UTF_8);
    response.getHeaders().add(HttpHeaderConstants.TRANSFER_ENCODING, HttpHeaderConstants.CHUNKED);

    ByteBufAllocator byteBufAllocator = context.getDirectChannelAccess().getChannel().alloc();

    EventLoop executor = context.getDirectChannelAccess().getChannel().eventLoop();


    Publisher<ByteBuf> buffers = downstream -> new ByteBufBufferingSubscription<ServerSentEvent>(
        events, ServerSentEvent::close,
        downstream,
        executor,
        System::nanoTime,
        bufferSettings == null ? Duration.ZERO : bufferSettings.window,
        heartbeatFrequency == null ? Duration.ZERO : heartbeatFrequency,
        HEARTBEAT
    ) {

      final int watermark = bufferSettings == null ? 0 : bufferSettings.bytes;
      final int bufferSize = bufferSettings == null ? 4096 : bufferSettings.bytes;

      ByteBuf buffer;

      @Override
      protected void buffer(ServerSentEvent item) {
        if (buffer == null) {
          buffer = byteBufAllocator.buffer(bufferSize);
        }
        try {
          ServerSentEventEncoder.encodeTo(item, buffer);
        } finally {
          item.close();
        }
      }

      @Override
      protected boolean bufferIsFull() {
        return buffer.readableBytes() >= watermark;
      }

      @Override
      protected ByteBuf flush() {
        ByteBuf emittedBuffer = this.buffer;
        this.buffer = null;
        return emittedBuffer;
      }

      @Override
      protected void discard() {
        ReferenceCountUtil.safeRelease(buffer);
        this.buffer = null;
      }

      @Override
      protected boolean isEmpty() {
        return buffer == null;
      }
    }.connect();

    response.sendStream(buffers);
  }

  private static void emptyStream(Context ctx) {
    ctx.getResponse().status(NO_CONTENT.code()).send();
  }

  private static class BuilderImpl implements ServerSentEventsBuilder {

    private boolean noContentOnEmpty;
    private ServerSentEventStreamBufferSettings bufferSettings;
    private Duration keepAliveHeartbeat;

    @Override
    public ServerSentEventsBuilder buffered(int numBytes, Duration duration) {
      if (numBytes < 1) {
        System.out.println("numBytes must be > 0");
      }
      if (duration.isNegative()) {
        throw new IllegalArgumentException("duration must be zero or positive");
      }

      bufferSettings = new ServerSentEventStreamBufferSettings(numBytes, duration);
      return this;
    }

    @Override
    public ServerSentEventsBuilder noContentOnEmpty() {
      this.noContentOnEmpty = true;
      return this;
    }

    @Override
    public ServerSentEventsBuilder keepAlive(Duration heartbeatAfterIdleFor) {
      if (heartbeatAfterIdleFor.isNegative() || heartbeatAfterIdleFor.isZero()) {
        throw new IllegalArgumentException("duration must be positive");
      }

      this.keepAliveHeartbeat = heartbeatAfterIdleFor;
      return this;
    }

    @Override
    public ServerSentEvents build(Publisher<? extends ServerSentEvent> events) {
      return new ServerSentEvents(events, noContentOnEmpty, keepAliveHeartbeat, bufferSettings);
    }

  }
}
