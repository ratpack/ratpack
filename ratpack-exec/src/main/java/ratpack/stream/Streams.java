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

package ratpack.stream;

import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import ratpack.exec.*;
import ratpack.exec.internal.DefaultExecution;
import ratpack.func.Action;
import ratpack.func.BiFunction;
import ratpack.func.Function;
import ratpack.func.Predicate;
import ratpack.registry.Registry;
import ratpack.stream.internal.*;
import ratpack.util.Types;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Some lightweight utilities for working with <a href="http://www.reactive-streams.org/">reactive streams</a>.
 * <blockquote>
 * <p>Reactive Streams is an initiative to provide a standard for asynchronous stream processing with non-blocking back pressure on the JVM.</p>
 * <p><a href="http://www.reactive-streams.org/">http://www.reactive-streams.org</a></p>
 * </blockquote>
 * <p>
 * Ratpack uses the Reactive Streams API when consuming streams of data (e.g {@link ratpack.http.Response#sendStream(Publisher)}).
 * </p>
 * <p>
 * This class provides some useful reactive utilities that integrate other parts of the Ratpack API with Reactive Stream types.
 * It is not designed to be a fully featured reactive toolkit.
 * If you require more features than provided here, consider using Ratpack's RxJava or Reactor integration.
 * </p>
 * <p>
 * The methods in this class are available as <a href="http://docs.groovy-lang.org/latest/html/documentation/#_extension_modules">Groovy Extensions</a>.
 * When using Groovy, applications can utilize the static methods provided in this class as instance-level methods against the first argument in their variable argument list.
 * </p>
 */
public class Streams {

  /**
   * Wraps the publisher in Ratpack's {@link TransformablePublisher} to make composing a pipeline easier.
   * <p>
   * The return publisher is effectively the same publisher in terms of the {@link Publisher#subscribe(org.reactivestreams.Subscriber)} method.
   *
   * @param publisher the publisher to wrap
   * @param <T> the type of item the publisher emits
   * @return a wrapped publisher
   */
  public static <T> TransformablePublisher<T> transformable(Publisher<T> publisher) {
    if (publisher instanceof TransformablePublisher) {
      return Types.cast(publisher);
    } else {
      return new DefaultTransformablePublisher<>(publisher);
    }
  }

  /**
   * An empty publisher that immediately completes.
   *
   * @param <T> the type of item expected
   * @return an empty publisher
   * @since 1.5
   */
  public static <T> TransformablePublisher<T> empty() {
    return Types.cast(EmptyPublisher.INSTANCE);
  }

  /**
   * Converts an iterable to a publishable.
   * <p>
   * Upon subscription, a new iterator will be created from the iterable.
   * Values are pulled from the iterator in accordance with the requests from the subscriber.
   * <p>
   * Any exception thrown by the iterable/iterator will be forwarded to the subscriber.
   *
   * @param iterable the data source
   * @param <T> the type of item emitted
   * @return a publisher for the given iterable
   */
  public static <T> TransformablePublisher<T> publish(Iterable<T> iterable) {
    return new IterablePublisher<>(iterable);
  }

  /**
   * Converts a {@link Promise} for an iterable into a publishable.
   * <p>
   * Upon subscription the promise will be consumed and the promised iterable will be emitted
   * to the subscriber one element at a time.
   * <p>
   * Any exception thrown by the the promise will be forwarded to the subscriber.
   *
   * @param promise the promise
   * @param <T> the element type of the promised iterable
   * @return a publisher for each element of the promised iterable
   * @since 1.1
   */
  public static <T> TransformablePublisher<T> publish(Promise<? extends Iterable<T>> promise) {
    return new IterablePromisePublisher<>(promise);
  }

  /**
   * Creates a new publisher, backed by the given data producing function.
   * <p>
   * As subscribers request data of the returned stream, the given function is invoked.
   * The function returns the item to send downstream.
   * If the function returns {@code null}, the stream is terminated.
   * If the function throws an exception, the stream is terminated and the error is sent downstream.
   * <pre class="java">{@code
   * import ratpack.stream.Streams;
   * import ratpack.test.exec.ExecHarness;
   *
   * import java.util.Arrays;
   * import java.util.List;
   * import static org.junit.Assert.*;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     List<String> strings = ExecHarness.yieldSingle(execControl ->
   *       Streams.yield(r -> {
   *         if (r.getRequestNum() < 2) {
   *           return Long.toString(r.getRequestNum());
   *         } else {
   *           return null;
   *         }
   *       }).toList()
   *     ).getValue();
   *
   *     assertEquals(Arrays.asList("0", "1"), strings);
   *   }
   * }
   * }</pre>
   * <p>
   * If the value producing function is asynchronous, use {@link #flatYield(Function)}.
   *
   * @param producer the data source
   * @param <T> the type of item emitted
   * @see #flatYield
   * @return a publisher backed by the given producer
   */
  public static <T> TransformablePublisher<T> yield(Function<? super YieldRequest, ? extends T> producer) {
    return new YieldingPublisher<>(producer);
  }

  /**
   * Creates a new publisher, backed by the given asynchronous data producing function.
   * <p>
   * As subscribers request data of the returned stream, the given function is invoked.
   * The function returns a promise for the item to send downstream.
   * If the promise provides a value of {@code null}, the stream is terminated.
   * If the promise produces an error, the stream is terminated and the error is sent downstream.
   * If the promise producing function throws an exception, the stream is terminated and the error is sent downstream.
   * <pre class="java">{@code
   * import ratpack.stream.Streams;
   * import ratpack.exec.Promise;
   * import ratpack.test.exec.ExecHarness;
   *
   * import java.util.Arrays;
   * import java.util.List;
   * import static org.junit.Assert.*;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     List<String> strings = ExecHarness.yieldSingle(execControl ->
   *       Streams.flatYield(r -> {
   *         if (r.getRequestNum() < 2) {
   *           return Promise.value(Long.toString(r.getRequestNum()));
   *         } else {
   *           return Promise.ofNull();
   *         }
   *       }).toList()
   *     ).getValue();
   *
   *     assertEquals(Arrays.asList("0", "1"), strings);
   *   }
   * }
   * }</pre>
   * <p>
   * If the value producing function is not asynchronous, use {@link #yield(Function)}.
   *
   * @param producer the data source
   * @param <T> the type of item emitted
   * @see #yield
   * @return a publisher backed by the given producer
   */
  public static <T> TransformablePublisher<T> flatYield(Function<? super YieldRequest, ? extends Promise<T>> producer) {
    return new FlatYieldingPublisher<>(producer);
  }

  /**
   * Creates a new publisher, that indefinitely streams the given object to all subscribers.
   * <p>
   * This is rarely useful for anything other than testing.
   *
   * @param item the item to indefinitely stream
   * @param <T> the type of item emitted
   * @return a publisher that indefinitely streams the given item
   */
  public static <T> TransformablePublisher<T> constant(final T item) {
    return yield(yieldRequest -> item);
  }

  /**
   * Returns a publisher that publishes items from the given input publisher after transforming each item via the given function.
   * <p>
   * The returned publisher does not perform any flow control on the data stream.
   * <p>
   * If the given transformation errors, the exception will be forwarded to the subscriber and the subscription to the input stream will be cancelled.
   *
   * @param input the stream of input data
   * @param function the transformation
   * @param <I> the type of input item
   * @param <O> the type of output item
   * @return a publisher that applies the given transformation to each item from the input stream
   */
  public static <I, O> TransformablePublisher<O> map(Publisher<I> input, Function<? super I, ? extends O> function) {
    return new MapPublisher<>(input, function);
  }

  /**
   * Returns a publisher that filters items from the given input stream by applying the given filter predicate.
   * <p>
   * The returned stream is {@link #buffer buffered}, which means that if the downstream requests, say 5 items, which is filtered into only 3 items
   * the publisher will ask for more from the upstream to meet the downstream demand.
   * <pre class="java">{@code
   * import org.reactivestreams.Publisher;
   * import ratpack.stream.Streams;
   * import ratpack.stream.TransformablePublisher;
   * import ratpack.test.exec.ExecHarness;
   *
   * import java.util.Arrays;
   * import java.util.List;
   *
   * import static org.junit.Assert.*;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     List<Integer> result = ExecHarness.yieldSingle(execControl -> {
   *       TransformablePublisher<Integer> evens = Streams.publish(Arrays.asList(1, 2, 3, 4, 5, 6)).filter(i -> i % 2 == 0);
   *       return evens.toList();
   *     }).getValue();
   *
   *     assertEquals(Arrays.asList(2, 4, 6), result);
   *   }
   * }
   * }</pre>
   *
   * @param input the stream to filter
   * @param filter the filter predicate
   * @param <T> the type of item emitted
   * @return the input stream filtered
   */
  public static <T> TransformablePublisher<T> filter(Publisher<T> input, Predicate<? super T> filter) {
    return streamMap(input, (s, out) -> out.itemMap(s, item -> {
      if (filter.apply(item)) {
        out.item(item);
      } else {
        s.request(1);
      }
    }));
  }

  /**
   * Allows transforming a stream into an entirely different stream.
   * <p>
   * While the {@link #map(Publisher, Function)} method support transforming individual items, this method supports transforming the stream as a whole.
   * This is necessary when the transformation causes a different number of items to be emitted than the original stream.
   * <pre class="java">{@code
   * import org.reactivestreams.Publisher;
   * import ratpack.stream.Streams;
   * import ratpack.stream.TransformablePublisher;
   * import ratpack.stream.WriteStream;
   * import ratpack.test.exec.ExecHarness;
   *
   * import java.util.Arrays;
   * import java.util.List;
   *
   * import static org.junit.Assert.*;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     List<String> result = ExecHarness.yieldSingle(execControl -> {
   *       Publisher<String> chars = Streams.publish(Arrays.asList("a", "b", "c"));
   *       TransformablePublisher<String> mapped = Streams.streamMap(chars, (subscription, out) ->
   *         new WriteStream<String>() {
   *           public void item(String item) {
   *             out.item(item);
   *             out.item(item.toUpperCase());
   *           }
   *
   *           public void error(Throwable error) {
   *             out.error(error);
   *           }
   *
   *           public void complete() {
   *             out.complete();
   *           }
   *         }
   *       );
   *       return mapped.toList();
   *     }).getValue();
   *
   *     assertEquals(Arrays.asList("a", "A", "b", "B", "c", "C"), result);
   *   }
   * }
   * }</pre>
   * <p>
   * The {@code mapper} function receives a {@link WriteStream} for emitting items and returns a {@link WriteStream} that will be written to by the upstream publisher.
   * Calling {@link WriteStream#complete()} or {@link WriteStream#error(Throwable)} on the received write stream will {@link org.reactivestreams.Subscription#cancel() cancel} the upstream subscription and it is guaranteed that no more items will be emitted from the upstream.
   * If the complete/error signals from upstream don't need to be intercepted, the {@link WriteStream#itemMap(Subscription, Action)} can be used on the write stream given to the mapping function to of the return write stream.
   * <p>
   * Implementations must take care to call {@link Subscription#cancel()} on the provided subscription if they introduce an error.
   * This is not required when simply forwarding an upstream error.
   * <p>
   * The returned stream is {@link #buffer buffered}, which allows the stream transformation to emit more items downstream than what was received from the upstream.
   * Currently, the upstream subscription will always receive a single {@link org.reactivestreams.Subscription#request(long) request} for {@link Long#MAX_VALUE}, effectively asking upstream to emit as fast as it can.
   * Future versions may propagate backpressure more intelligently.
   *
   * @param input the stream to map
   * @param mapper the mapping function
   * @param <U> the type of item received
   * @param <D> the type of item produced
   * @return the input stream transformed
   * @since 1.4
   */
  public static <U, D> TransformablePublisher<D> streamMap(Publisher<? extends U> input, StreamMapper<? super U, D> mapper) {
    return new StreamMapPublisher<>(input, mapper).buffer();
  }

  /**
   * @deprecated since 1.4, use {@link #streamMap(Publisher, StreamMapper)}
   */
  @Deprecated
  public static <U, D> TransformablePublisher<D> streamMap(Publisher<U> input, Function<? super WriteStream<D>, ? extends WriteStream<? super U>> mapper) {
    return streamMap(input, (subscription, downstream) -> {
      @SuppressWarnings("UnnecessaryLocalVariable")
      WriteStream<? super U> writeStream = mapper.apply(downstream);
      return writeStream;
    });
  }

  /**
   * Returns a publisher that publishes items from the given input publisher after transforming each item via the given, promise returning, function.
   * <p>
   * The returned publisher does not perform any flow control on the data stream.
   * <p>
   * If the given transformation errors, or if the returned promise fails, the exception will be forwarded to the subscriber and the subscription to the input stream will be cancelled.
   *
   * @param input the stream of input data
   * @param function the transformation
   * @param <I> the type of input item
   * @param <O> the type of output item
   * @return a publisher that applies the given transformation to each item from the input stream
   */
  public static <I, O> TransformablePublisher<O> flatMap(Publisher<I> input, Function<? super I, ? extends Promise<? extends O>> function) {
    return new BatchingPublisher<>(new FlatMapPublisher<>(input, function), 1, Action.noop());
  }

  /**
   * Returns a publisher that allows the given publisher to without respecting demand.
   * <p>
   * The given publisher may violate the Reactive Streams contract in that it may emit more items than have been requested.
   * Any excess will be buffered until there is more demand.
   * All requests for items from the subscriber will be satisfied from the buffer first.
   * If a request is made at any time for more items than are currently in the buffer,
   * a request for the unmet demand will be made of the given publisher.
   * <p>
   * If the given producer emits far faster than the downstream subscriber requests, the intermediate queue will grow large and consume memory.
   *
   * @param publisher a data source
   * @param <T> the type of item
   * @return a publisher that buffers items emitted by the given publisher that were not requested
   */
  public static <T> TransformablePublisher<T> buffer(Publisher<T> publisher) {
    return new BufferingPublisher<>(Action.noop(), publisher);
  }

  /**
   * Allows requests from the subscriber of the return publisher to be withheld from the given publisher until an externally defined moment.
   * <p>
   * When the return publisher is subscribed to, the given publisher will be subscribed to.
   * All requests made by the subscriber of the return publisher will not be forwarded to the subscription of the given publisher until the runnable given to the given action is run.
   * Once the runnable is run, all requests are directly forwarded to the subscription of the given publisher.
   * <p>
   * The return publisher supports multi subscription, creating a new subscription to the given publisher each time.
   * The given action will be invoke each time the return publisher is subscribed to with a distinct runnable for releasing the gate for that subscription.
   * <p>
   * The given action will be invoked immediately upon subscription of the return publisher.
   * The runnable given to this action may be invoked any time (i.e. it does not need to be run during the action).
   * If the action errors, the given publisher will not be subscribed to and the error will be sent to the return publisher subscriber.
   *
   * @param publisher the data source
   * @param valveReceiver an action that receives a runnable “valve” that when run allows request to start flowing upstream
   * @param <T> the type of item emitted
   * @return a publisher that is logically equivalent to the given publisher as far as subscribers are concerned
   */
  public static <T> TransformablePublisher<T> gate(Publisher<T> publisher, Action<? super Runnable> valveReceiver) {
    return new GatedPublisher<>(publisher, valveReceiver);
  }

  /**
   * Executes the given function periodically, publishing the return value to the subscriber.
   * <p>
   * When the return publisher is subscribed to, the given function is executed immediately (via the executor) with {@code 0} as the input.
   * The function will then be repeatedly executed again after the given delay (with an incrementing input) until the function returns {@code null}.
   * That is, a return value from the function of {@code null} signals that the data stream has finished.
   * The function will not be executed again after returning {@code null}.
   * <p>
   * Each new subscription to the publisher will cause the function to be scheduled again.
   * Due to this, it is generally desirable to wrap the return publisher in a multicasting publisher.
   * <p>
   * If the function throws an exception, the error will be sent to the subscribers and no more invocations of the function will occur.
   * <p>
   * The returned publisher is implicitly buffered to respect back pressure via {@link #buffer(Publisher)}.
   *
   * @param executorService the executor service that will periodically execute the function
   * @param duration the duration of the delay (Duration.ofSeconds(1) - delay will be 1 second)
   * @param producer a function that produces values to emit
   * @param <T> the type of item
   * @return a publisher that applies respects back pressure, effectively throttling the given publisher
   */
  public static <T> TransformablePublisher<T> periodically(ScheduledExecutorService executorService, Duration duration, Function<? super Integer, ? extends T> producer) {
    return new PeriodicPublisher<T>(executorService, producer, duration).buffer();
  }

  public static <T> TransformablePublisher<T> periodically(Registry registry, Duration duration, Function<? super Integer, ? extends T> producer) {
    return new PeriodicPublisher<T>(registry.get(ExecController.class).getExecutor(), producer, duration).buffer();
  }

  /**
   * Allows listening to the events of the given publisher as they flow to subscribers.
   * <p>
   * When the return publisher is subscribed to, the given publisher will be subscribed to.
   * All events (incl. data, error and completion) emitted by the given publisher will be forwarded to the given listener before being forward to the subscriber of the return publisher.
   * <p>
   * If the listener errors, the upstream subscription will be cancelled (if appropriate) and the error sent downstream.
   * If the listener errors while listening to an error event, the listener error will be {@link Throwable#addSuppressed(Throwable) added as a surpressed exception}
   * to the original exception which will then be sent downstream.
   *
   * @param publisher the data source
   * @param listener the listener for emitted items
   * @param <T> the type of item emitted
   * @return a publisher that is logically equivalent to the given publisher as far as subscribers are concerned
   */
  public static <T> TransformablePublisher<T> wiretap(Publisher<T> publisher, Action<? super StreamEvent<T>> listener) {
    return new WiretapPublisher<>(publisher, listener);
  }

  /**
   * Returns a publisher that will stream events emitted from the given publisher to all of its subscribers.
   * <p>
   * The return publisher allows the given publisher to emit as fast as it can, while applying flow control downstream to multiple subscribers.
   * Each subscriber can signal its own demand.  If the given publisher emits far faster than the downstream subscribers request, the intermediate
   * queue of each subscriber will grow large and consume substantial memory. However, given this publisher is likely to be used with a periodic
   * publisher or a regular indefinite stream it is unlikely to be a problem.
   * <p>
   * When a subscriber subscribes to the return publisher then it will not receive any events that have been emitted before it subscribed.
   *
   * @param publisher a data source
   * @param <T> the type of item
   * @return a publisher that respects back pressure for each of it's Subscribers.
   */
  public static <T> TransformablePublisher<T> multicast(Publisher<T> publisher) {
    return new MulticastPublisher<>(publisher);
  }

  /**
   * Returns a publisher that publishes each element from Collections that are produced from the given input publisher.
   * <p>
   * For each item the return publisher receives from the given input publisher, the return publisher will iterate over its elements and publish a
   * new item for each element to its downstream subscriber e.g. if the return publisher receives a Collection with 10 elements then the downstream
   * subscriber will receive 10 calls to its onNext method.
   * <p>
   * The returned publisher is implicitly buffered to respect back pressure via {@link #buffer(Publisher)}.
   *
   * @param publisher the data source
   * @param <T> the type of item emitted
   * @return a publisher that splits collection items into new items per collection element
   */
  public static <T> TransformablePublisher<T> fanOut(Publisher<? extends Iterable<? extends T>> publisher) {
    return fanOut(publisher, Action.noop());
  }

  /**
   * Returns a publisher that publishes each element from Collections that are produced from the given input publisher.
   * <p>
   * For each item the return publisher receives from the given input publisher, the return publisher will iterate over its elements and publish a
   * new item for each element to its downstream subscriber e.g. if the return publisher receives a Collection with 10 elements then the downstream
   * subscriber will receive 10 calls to its onNext method.
   * <p>
   * The returned publisher is implicitly buffered to respect back pressure via {@link #buffer(Publisher)}.
   *
   * @param publisher the data source
   * @param disposer the disposer of unhandled items
   * @param <T> the type of item emitted
   * @return a publisher that splits collection items into new items per collection element
   * @since 1.5
   */
  public static <T> TransformablePublisher<T> fanOut(Publisher<? extends Iterable<? extends T>> publisher, Action<? super T> disposer) {
    return new FanOutPublisher<>(publisher, disposer);
  }

  /**
   * Returns a publisher that merges the given input publishers into a single stream of elements.
   * <p>
   * The returned publisher obeys the following rules:
   * <ul>
   *   <li>
   *    Only when all given input publishers have signalled completion will the downstream subscriber be completed.
   *   </li>
   *   <li>
   *    If one of the given input publishers errors then all other publisher subscriptions are cancelled and the error is propagated to the subscriber.
   *   </li>
   *   <li>
   *    If the subscription of the returned publisher is cancelled by the subscriber then all given input publisher subscriptions are cancelled.
   *   </li>
   * </ul>
   * <p>
   * The returned publisher is implicitly buffered to respect back pressure via {@link #buffer(org.reactivestreams.Publisher)}.
   *
   * @param publishers the data sources to merge
   * @param <T> the type of item emitted
   * @return a publisher that emits a single stream of elements from multiple publishers
   */
  @SuppressWarnings({"unchecked", "varargs"})
  @SafeVarargs
  public static <T> TransformablePublisher<T> merge(Publisher<? extends T>... publishers) {
    return new MergingPublisher<>(publishers).buffer();
  }

  /**
   * Creates a promise for the given publisher's single item.
   * <p>
   * The given publisher is expected to produce zero or one items.
   * If it produces zero, the promised value will be {@code null}.
   * The it produces exactly one item, the promised value will be that item.
   * <p>
   * If the stream produces more than one item, the promise will fail with an {@link IllegalStateException}.
   * As soon as a second item is received, the subscription to the given publisher will be cancelled.
   * <p>
   * The single item is not provided to the promise subscriber until the stream completes, to ensure that it is indeed a one element stream.
   * If the stream errors before sending a second item, the promise will fail with that error.
   * If it fails after sending a second item, that error will be ignored.
   *
   * @param publisher the publiser the convert to a promise
   * @param <T> the type of promised value
   * @return a promise for the publisher's single item
   */
  public static <T> Promise<T> toPromise(Publisher<T> publisher) {
    return Promise.async(f -> publisher.subscribe(SingleElementSubscriber.to(f::accept)));
  }

  /**
   * Creates a promise for the given publisher's items as a List.
   *
   * @param publisher the stream to collect to a list
   * @param <T> the type of item in the stream
   * @return a promise for the streams contents as a list
   */
  public static <T> Promise<List<T>> toList(Publisher<T> publisher) {
    return Promise.async(f -> publisher.subscribe(new CollectingSubscriber<>(f::accept, s -> s.request(Long.MAX_VALUE))));
  }

  /**
   * Reduces the stream to a single value, by applying the given function successively.
   *
   * @param publisher the publisher to reduce
   * @param seed the initial value
   * @param reducer the reducing function
   * @param <R> the type of result
   * @return a promise for the reduced value
   * @since 1.4
   */
  public static <T, R> Promise<R> reduce(Publisher<T> publisher, R seed, BiFunction<? super R, ? super T, ? extends R> reducer) {
    return Promise.async(d ->
      publisher.subscribe(new Subscriber<T>() {
        private Subscription subscription;
        private volatile R value = seed;
        private AtomicInteger count = new AtomicInteger();

        @Override
        public void onSubscribe(Subscription s) {
          subscription = s;
          s.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(T t) {
          count.incrementAndGet();
          try {
            value = reducer.apply(value, t);
          } catch (Throwable e) {
            subscription.cancel();
            d.error(e);
          }
        }

        @Override
        public void onError(Throwable t) {
          d.error(t);
        }

        @Override
        public void onComplete() {
          d.success(value);
        }
      })
    );
  }

  /**
   * Binds the given publisher to the current {@link Execution}.
   * <p>
   * Calls {@link #bindExec(Publisher, Action)} with {@link Action#noop()} as the disposer.
   *
   * @param publisher the publisher to bind to the execution
   * @param <T> the type of item emitted by the publisher
   * @return a new publisher that binds the given publisher to the current execution
   */
  public static <T> TransformablePublisher<T> bindExec(Publisher<T> publisher) {
    return bindExec(publisher, Action.noop());
  }

  /**
   * Binds the given publisher to the current {@link Execution}.
   * <p>
   * Publishers may emit signals asynchronously and on any thread.
   * An execution bound publisher emits all of its “signals” (e.g. {@code onNext()}) on its execution (and therefore same thread).
   * By binding the publisher to the execution, the execution can remain open while the publisher is emitting
   * and subscribers receive signals within the execution and can therefore use {@link Promise} etc
   * and have the appropriate execution state and error handling.
   * <p>
   * There is a performance overhead in binding a publisher to an execution.
   * It is typically only necessary to bind the last publisher in a chain to the execution.
   * If the processing of items does not require execution mechanics, it can be faster to wrap the publisher subscription
   * in {@link Promise#async(Upstream)} and complete the promise in the subscriber's {@link Subscriber#onComplete()}.
   * <p>
   * The given {@code disposer} is used to “free” any items that were not yet received by the subscriber when
   * the subscription is cancelled, or if the subscriber errors.
   * This is only required if the emitted items are reference counted (e.g. {@link ByteBuf}) or hold open resources (e.g. file handles).
   * Any exceptions raised by the disposer will be logged then ignored.
   * If items do not need disposing, pass {@link Action#noop()}.
   *
   * @param publisher the publisher to bind to the execution
   * @param disposer the disposer of unhandled items
   * @param <T> the type of item emitted by the publisher
   * @return a new publisher that binds the given publisher to the current execution
   * @since 1.5
   */
  public static <T> TransformablePublisher<T> bindExec(Publisher<T> publisher, Action<? super T> disposer) {
    return DefaultExecution.stream(publisher, disposer);
  }

  /**
   * Consumes the given publisher eagerly in a forked execution, buffering results until ready to be consumed by this execution.
   * <p>
   * This can be used when wanting to effectively parallelize the production of values and the consumption.
   * The given publisher can emit items as fast as possible, independent of consumption.
   * <p>
   * If the given publisher emits faster than the consumer of the returned publisher,
   * excessive memory may be used to buffer the items until the consumer can process them.
   * <p>
   * The given publisher will not be subscribed to until the returned publisher is.
   * When the first {@link Subscription#request(long)} is issued,
   * a request for {@link Long#MAX_VALUE} will be issued to the subscription to the given publisher.
   * <p>
   * The returned publisher is {@link #bindExec(Publisher) execution bound}.
   *
   * @param publisher the publisher to consume as fast as possible in a forked execution
   * @param execConfig the configuration for the forked execution
   * @param disposer the disposer for any buffered items when the stream errors or is cancelled
   * @param <T> the type of emitted item
   * @return an execution bound publisher that propagates the items of the given publisher
   * @since 1.5
   */
  public static <T> TransformablePublisher<T> fork(Publisher<T> publisher, Action<? super ExecSpec> execConfig, Action<? super T> disposer) {
    return new BufferingPublisher<T>(disposer, write -> {
      return new ForkingSubscription<>(execConfig, publisher, write);
    }).bindExec(disposer);
  }

  /**
   * Returns a publisher that emits only the first {@code n} elements from the given publisher, where {@code n} is the
   * given count.
   * <p>
   * On emitting the required number of elements, the upstream subscription will be cancelled and the stream completed.
   * <p>
   * The given required number of elements is a maximum. If the upstream publisher completes before the required number
   * of elements is reached, then the stream completes as normal.
   *
   * @param count the max number of items to emit before completing
   * @param upstreamPublisher the publisher to take from
   * @param <T> the type of emitted item
   * @return a publisher that will emit a max of {@code n} elements
   * @since 1.5
   */
  public static <T> TransformablePublisher<T> take(long count, Publisher<T> upstreamPublisher) {
    return new TakePublisher<>(count, upstreamPublisher);
  }

  /**
   * Returns a publisher that aggregates the given publishers into a single stream of elements, without interleaving them.
   * <p>
   * The returned publisher obeys the following rules:
   * <ul>
   *   <li>
   *    Given publishers are subscribed to lazily and in the order they are supplied.
   *    That is, a publisher is not subscribed to until the previous publisher has completed.
   *   </li>
   *   <li>
   *    Elements emitted from the given publishers are not interleaved.
   *   </li>
   *   <li>
   *    Only when all given publishers have signalled completion will the downstream subscriber be completed.
   *   </li>
   * </ul>
   * <p>
   *
   * @param <T> the type of emitted item
   * @param publishers the publishers to concatenate
   * @param disposer the disposer of unhandled items
   * @return a publisher that emits a single stream of elements from multiple publishers
   * @since 1.5
   */
  public static <T> TransformablePublisher<T> concat(Iterable<? extends Publisher<? extends T>> publishers, Action<? super T> disposer) {
    return new ConcatPublisher<>(disposer, publishers);
  }

  /**
   * Similar to {@link #concat(Iterable, Action)}, but with a noop disposer.
   *
   * @param publishers the publishers to concatenate.
   * @param <T> the type of emitted item
   * @return a publisher that emits a single stream of elements from multiple publishers
   * @since 1.5
   */
  public static <T> TransformablePublisher<T> concat(Iterable<? extends Publisher<? extends T>> publishers) {
    return new ConcatPublisher<>(Action.noop(), publishers);
  }

  /**
   * Batches and serialised demand.
   * <p>
   * Subscribers often request items one at a time.
   * This can cause inefficient data fetching patterns in publishers (e.g fetching one row at a time from a database result set).
   * Such publishers can be wrapped in a batch publisher, which transforms demand into regular sizes.
   * <p>
   * Excess items are buffered until the subscriber wants them.
   * Therefore, using a very large batch size with a very slow subscriber may require significant memory.
   *
   * @param batchSize the batch size
   * @param publisher the publisher to issue request to in batches
   * @param disposer the disposer of unhandled items (e.g. buffered, unwanted, items)
   * @param <T> the type of emitted item
   * @return a publisher that batches requests upstream
   * @since 1.5
   */
  public static <T> TransformablePublisher<T> batch(int batchSize, Publisher<T> publisher, Action<? super T> disposer) {
    return new BatchingPublisher<>(publisher, batchSize, disposer);
  }

  /**
   * Creates a single publisher from a publisher of publishers.
   * <p>
   * Delegates to {@link #flatten(Publisher, Action)} with {@link Action#noop()}.
   *
   * @param publisher the publisher of publishers
   * @param <T> the type of emitted item
   * @return a publisher that flattens the given publisher
   * @since 1.5
   */
  public static <T> TransformablePublisher<T> flatten(Publisher<? extends Publisher<T>> publisher) {
    return flatten(publisher, Action.noop());
  }

  /**
   * Creates a single publisher from a publisher of publishers.
   * <p>
   * Each emitted publisher is delegated to until it completes,
   * upon which the next publisher will be requested and the actual demand delegated to it and so forth.
   *
   * <pre class="java">{@code
   * import org.reactivestreams.Publisher;
   * import ratpack.stream.Streams;
   * import ratpack.test.exec.ExecHarness;
   *
   * import java.util.List;
   *
   * import static java.util.Arrays.asList;
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *
   *   public static void main(String... args) throws Exception {
   *     List<Integer> value = ExecHarness.yieldSingle(e -> {
   *       Publisher<Integer> p1 = Streams.publish(asList(1, 2));
   *       Publisher<Integer> p2 = Streams.publish(asList(3, 4));
   *       Publisher<Publisher<Integer>> nested = Streams.publish(asList(p1, p2));
   *
   *       Publisher<Integer> flattened = Streams.flatten(nested);
   *       return Streams.toList(flattened);
   *     }).getValueOrThrow();
   *
   *     assertEquals(asList(1, 2, 3, 4), value);
   *   }
   * }
   * }</pre>
   *
   * @param publisher the publisher of publishers
   * @param disposer the disposer of unhandled items (e.g. buffered, unwanted, items)
   * @param <T> the type of emitted item
   * @return a publisher that flattens the given publisher
   * @since 1.5
   */
  public static <T> TransformablePublisher<T> flatten(Publisher<? extends Publisher<T>> publisher, Action<? super T> disposer) {
    return new FlattenPublisher<>(publisher, disposer);
  }

}
