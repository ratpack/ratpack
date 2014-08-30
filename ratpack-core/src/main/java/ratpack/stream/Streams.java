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

import org.reactivestreams.Publisher;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.stream.internal.*;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Some lightweight utilities for working with <a href="http://www.reactive-streams.org/">reactive streams</a>.
 * <blockquote>
 * <p>Reactive Streams is an initiative to provide a standard for asynchronous stream processing with non-blocking back pressure on the JVM.</p>
 * <p><a href="http://www.reactive-streams.org/">http://www.reactive-streams.org</a></p>
 * </blockquote>
 * <p>
 * Ratpack uses the Reactive Streams API when consuming streams of data (e.g {@link ratpack.http.Response#sendStream(ratpack.exec.ExecControl, org.reactivestreams.Publisher)}).
 * </p>
 * <p>
 * This class provides some useful reactive utilities that integrate other parts of the Ratpack API with Reactive Stream types.
 * It is not designed to be a fully featured reactive toolkit.
 * If you require more features than provided here, consider using Ratpack's RxJava or Reactor integration.
 * </p>
 */
public class Streams {

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
  public static <T> Publisher<T> publisher(Iterable<T> iterable) {
    return new IterablePublisher<>(iterable);
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
  public static <I, O> Publisher<O> transform(final Publisher<I> input, final Function<? super I, ? extends O> function) {
    return new TransformingPublisher<>(input, function);
  }

  /**
   * Returns a publisher that allows the given publisher to emit as fast as it can, while applying flow control downstream.
   * <p>
   * When the return publisher is subscribed to, a subscription will be made to the given publisher with a request for {@link Integer#MAX_VALUE} items.
   * This effectively allows the given publisher to emit each item as soon as it can.
   * The return publisher will manage the back pressure by holding excess items from the given publisher in memory until the downstream subscriber is ready for them.
   * <p>
   * This is a simple, naive, flow control mechanism.
   * If the given producer emits far faster than the downstream subscriber requests, the intermediate queue will grow large and consume substantial memory.
   * However, it is useful or adapting non-infinite publishers that cannot meaningfully respect back pressure.
   *
   * @param publisher a data source
   * @param <T> the type of item
   * @return a publisher that applies respects back pressure, effectively throttling the given publisher
   */
  public static <T> Publisher<T> throttle(final Publisher<T> publisher) {
    return new BufferingPublisher<>(publisher);
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
   * The returned publisher is implicitly throttled to respect back pressure via {@link #throttle(org.reactivestreams.Publisher)}.
   *
   * @param executorService the executor service that will periodically execute the function
   * @param delay the delay value
   * @param timeUnit the delay time unit
   * @param producer a function that produces values to emit
   * @param <T> the type of item
   * @return a publisher that applies respects back pressure, effectively throttling the given publisher
   */
  public static <T> Publisher<T> periodically(ScheduledExecutorService executorService, final long delay, final TimeUnit timeUnit, final Function<Integer, T> producer) {
    return throttle(new PeriodicPublisher<>(executorService, producer, delay, timeUnit));
  }

  /**
   * Allows listening to values being published without changing them.
   * <p>
   * When the return publisher is subscribed to, the given publisher will be subscribed to and all requests for items will be forwarded unaltered.
   * Each item emitted by the given publisher will be forwarded to the given listener, then sent downstream.
   * <p>
   * If the listener errors, the upstream subscription will be cancelled and the error sent downstream.
   *
   * @param publisher the data source
   * @param listener the listener for emitted items
   * @param <T> the type of item emitted
   * @return a publisher that is logically equivalent to the given publisher as far as subscribers are concerned
   */
  public static <T> Publisher<T> wiretap(final Publisher<T> publisher, final Action<? super T> listener) {
    return new WiretapPublisher<>(publisher, listener);
  }

  /**
   * Allows listening to subscription cancellation.
   * <p>
   * When the return publisher is subscribed to, the given publisher will be subscribed to and all requests for items will be forwarded unaltered.
   * Each item emitted by the given publisher will be forwarded downstream unaltered.
   * <p>
   * If the downstream subscriber issues a cancel request, the given action will be invoked with {@code null} as the argument.
   *
   * @param publisher the data source
   * @param listener the listener to notify if the subscription is cancelled
   * @param <T> the type of item emitted
   * @return a publisher that is logically equivalent to the given publisher as far as subscribers are concerned
   */
  public static <T> Publisher<T> onCancel(final Publisher<T> publisher, final Action<Void> listener) {
    return new CancellationListeningPublisher<>(publisher, listener);
  }

}
