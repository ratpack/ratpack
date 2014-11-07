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
import ratpack.exec.ExecControl;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.launch.LaunchConfig;
import ratpack.stream.internal.*;
import ratpack.util.Types;

import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Some lightweight utilities for working with <a href="http://www.reactive-streams.org/">reactive streams</a>.
 * <blockquote>
 * <p>Reactive Streams is an initiative to provide a standard for asynchronous stream processing with non-blocking back pressure on the JVM.</p>
 * <p><a href="http://www.reactive-streams.org/">http://www.reactive-streams.org</a></p>
 * </blockquote>
 * <p>
 * Ratpack uses the Reactive Streams API when consuming streams of data (e.g {@link ratpack.http.Response#sendStream(org.reactivestreams.Publisher)}).
 * </p>
 * <p>
 * This class provides some useful reactive utilities that integrate other parts of the Ratpack API with Reactive Stream types.
 * It is not designed to be a fully featured reactive toolkit.
 * If you require more features than provided here, consider using Ratpack's RxJava or Reactor integration.
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
    return transformable(new IterablePublisher<>(iterable));
  }

  /**
   * Creates a new publisher, backed by the given data producing function.
   *
   * @param producer the data source
   * @param <T> the type of item emitted
   * @return a publisher backed by the given producer
   */
  public static <T> TransformablePublisher<T> yield(Function<? super YieldRequest, T> producer) {
    return transformable(new YieldingPublisher<>(producer));
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
    return transformable(yield(yieldRequest -> item));
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
    return transformable(new MapPublisher<>(input, function));
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
    return transformable(new FlatMapPublisher<>(input, function));
  }

  /**
   * Returns a publisher that allows the given publisher to emit as fast as it can, while applying flow control downstream.
   * <p>
   * When the return publisher is subscribed to, a subscription will be made to the given publisher with a request for {@link Long#MAX_VALUE} items.
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
  public static <T> TransformablePublisher<T> buffer(Publisher<T> publisher) {
    return transformable(new BufferingPublisher<>(publisher));
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
    return transformable(new GatedPublisher<>(publisher, valveReceiver));
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
   * The returned publisher is implicitly buffered to respect back pressure via {@link #buffer(org.reactivestreams.Publisher)}.
   *
   * @param executorService the executor service that will periodically execute the function
   * @param delay the delay value
   * @param timeUnit the delay time unit
   * @param producer a function that produces values to emit
   * @param <T> the type of item
   * @return a publisher that applies respects back pressure, effectively throttling the given publisher
   */
  public static <T> TransformablePublisher<T> periodically(ScheduledExecutorService executorService, long delay, TimeUnit timeUnit, Function<Integer, T> producer) {
    return buffer(new PeriodicPublisher<>(executorService, producer, delay, timeUnit));
  }

  public static <T> TransformablePublisher<T> periodically(LaunchConfig launchConfig, long delay, TimeUnit timeUnit, Function<Integer, T> producer) {
    return buffer(new PeriodicPublisher<>(launchConfig.getExecController().getExecutor(), producer, delay, timeUnit));
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
  public static <T> TransformablePublisher<T> wiretap(Publisher<T> publisher, Action<? super StreamEvent<? super T>> listener) {
    return transformable(new WiretapPublisher<>(publisher, listener));
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
    return transformable(new MulticastPublisher<>(publisher));
  }

  /**
   * Returns a publisher that publishes each element from Collections that are produced from the given input publisher.
   * <p>
   * For each item the return publisher receives from the given input publisher, the return publisher will iterate over its elements and publish a
   * new item for each element to its downstream subscriber e.g. if the return publisher receives a Collection with 10 elements then the downstream
   * subscriber will receive 10 calls to its onNext method.
   * <p>
   * The returned publisher is implicitly buffered to respect back pressure via {@link #buffer(org.reactivestreams.Publisher)}.
   *
   * @param publisher the data source
   * @param <T> the type of item emitted
   * @return a publisher that splits collection items into new items per collection element
   */
  public static <T> TransformablePublisher<T> fanOut(Publisher<Collection<T>> publisher) {
    return buffer(new FanOutPublisher<>(publisher));
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
    return buffer(new MergingPublisher<>(publishers));
  }

  /**
   * Calls {@link #toPromise(ExecControl, Publisher)} with {@link ExecControl#current()} and the given publisher.
   *
   * @param publisher the publiser the convert to a promise
   * @param <T> the type of promised value
   * @return a promise for the publisher's single item
   */
  public static <T> Promise<T> toPromise(Publisher<T> publisher) {
    return toPromise(ExecControl.current(), publisher);
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
   * @param execControl the exec control to create the promise from
   * @param publisher the publisher to extract the promised item from
   * @param <T> the type of promised value
   * @return a promise for the publisher's single item
   * @see #toPromise(Publisher)
   */
  public static <T> Promise<T> toPromise(ExecControl execControl, Publisher<T> publisher) {
    return execControl.promise(f -> publisher.subscribe(SingleElementSubscriber.to(f::accept)));
  }

}
