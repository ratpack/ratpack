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
import org.reactivestreams.Subscriber;
import ratpack.exec.ExecSpec;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.func.BiFunction;
import ratpack.func.Function;
import ratpack.func.Predicate;

import java.util.List;

/**
 * A wrapper over a {@link Publisher} that makes it more convenient to chain transformations of different kinds.
 * <p>
 * Note that this type implements the publisher interface,
 * so behaves just like the publisher that it is wrapping with respect to the
 * {@link Publisher#subscribe(Subscriber)} method.
 *
 * @param <T> the type of item emitted by this publisher
 */
public interface TransformablePublisher<T> extends Publisher<T> {

  /**
   * See {@link Streams#map(Publisher, Function)}.
   *
   * @param function the transformation
   * @param <O> the type of transformed item
   * @return the transformed publisher
   */
  default <O> TransformablePublisher<O> map(Function<? super T, ? extends O> function) {
    return Streams.map(this, function);
  }

  /**
   * See {@link Streams#flatMap(Publisher, Function)}.
   *
   * @param function the transformation
   * @param <O> the type of transformed item
   * @return the transformed publisher
   */
  default <O> TransformablePublisher<O> flatMap(Function<? super T, ? extends Promise<? extends O>> function) {
    return Streams.flatMap(this, function);
  }

  /**
   * See {@link Streams#buffer(Publisher)}.
   *
   * @return a buffering publisher
   */
  default TransformablePublisher<T> buffer() {
    return Streams.buffer(this);
  }

  /**
   * See {@link Streams#gate(Publisher, Action)}.
   *
   * @param valveReceiver an action that receives a runnable “valve” that when run allows request to start flowing upstream
   * @return a publisher that is logically equivalent to the given publisher as far as subscribers are concerned
   */
  default TransformablePublisher<T> gate(Action<? super Runnable> valveReceiver) {
    return Streams.gate(this, valveReceiver);
  }

  /**
   * See {@link Streams#wiretap(Publisher, Action)}.
   *
   * @param listener the listener for emitted items
   * @return a publisher that is logically equivalent to the given publisher as far as subscribers are concerned
   */
  default TransformablePublisher<T> wiretap(Action<? super StreamEvent<T>> listener) {
    return Streams.wiretap(this, listener);
  }

  /**
   * See {@link Streams#multicast(Publisher)}.
   *
   * @return a publisher that respects back pressure for each of its subscribers
   */
  default TransformablePublisher<T> multicast() {
    return Streams.multicast(this);
  }

  /**
   * See {@link Streams#toPromise(Publisher)}.
   *
   * @return a promise for this publisher's single item
   */
  default Promise<T> toPromise() {
    return Streams.toPromise(this);
  }

  /**
   * Consumes the given publisher's items to a list.
   * <p>
   * This method can be useful when testing, but should be uses with care in production code as it will exhaust memory if the stream is very large.
   * <pre class="java">{@code
   * import org.reactivestreams.Publisher;
   * import ratpack.stream.Streams;
   * import ratpack.test.exec.ExecHarness;
   *
   * import java.util.Arrays;
   * import java.util.List;
   *
   * import static org.junit.Assert.*;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     List<String> expected = Arrays.asList("a", "b", "c");
   *     List<String> result = ExecHarness.yieldSingle(execControl ->
   *       Streams.publish(expected).toList()
   *     ).getValue();
   *
   *     assertEquals(Arrays.asList("a", "b", "c"), result);
   *   }
   * }
   * }</pre>
   * <p>
   * If the publisher emits an error, the promise will fail and the collected items will be discarded.
   * <pre class="java">{@code
   * import org.reactivestreams.Publisher;
   * import ratpack.stream.Streams;
   * import ratpack.test.exec.ExecHarness;
   *
   * import static org.junit.Assert.*;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     Throwable error = ExecHarness.yieldSingle(execControl ->
   *       Streams.yield(r -> {
   *         if (r.getRequestNum() < 1) {
   *           return "a";
   *         } else {
   *           throw new RuntimeException("bang!");
   *         }
   *       }).toList()
   *     ).getThrowable();
   *
   *     assertEquals("bang!", error.getMessage());
   *   }
   * }
   * }</pre>
   *
   * @return a promise for the stream's contents as a list
   */
  default Promise<List<T>> toList() {
    return Streams.toList(this);
  }

  /**
   * Convenience method to allow a non Ratpack publisher transform method to be hooked in.
   * <p>
   * This transformable publisher will be given to the function, that should return a new publisher.
   * The returned publisher will then be wrapped in a transformable wrapper which will be returned by this method.
   *
   * @param transformer a publisher transformer
   * @param <O> the type of transformed item
   * @return a publisher that respects back pressure for each of its subscribers
   */
  default <O> TransformablePublisher<O> transform(java.util.function.Function<? super TransformablePublisher<? extends T>, ? extends Publisher<O>> transformer) {
    return Streams.transformable(transformer.apply(this));
  }

  /**
   * See {@link Streams#streamMap(Publisher, StreamMapper)}.
   *
   * @param mapper the transformation
   * @param <O> the type of transformed item
   * @return the transformed publisher
   * @since 1.4
   */
  default <O> TransformablePublisher<O> streamMap(StreamMapper<? super T, O> mapper) {
    return Streams.streamMap(this, mapper);
  }

  /**
   * @deprecated since 1.4, use {@link #streamMap(StreamMapper)}
   */
  @Deprecated
  default <O> TransformablePublisher<O> streamMap(Function<? super WriteStream<O>, ? extends WriteStream<? super T>> function) {
    return Streams.streamMap(this, function);
  }

  /**
   * See {@link Streams#filter(Publisher, Predicate)}.
   *
   * @param filter the filter
   * @return the filtered publisher
   */
  default TransformablePublisher<T> filter(Predicate<? super T> filter) {
    return Streams.filter(this, filter);
  }

  /**
   * See {@link Streams#bindExec(Publisher)}
   *
   * @return a publisher bound to the current execution
   * @since 1.4
   */
  default TransformablePublisher<T> bindExec() {
    return Streams.bindExec(this);
  }

  /**
   * See {@link Streams#bindExec(Publisher, Action)}
   *
   * @param disposer the disposer of unhandled items
   * @return a publisher bound to the current execution
   * @since 1.5
   */
  default TransformablePublisher<T> bindExec(Action<? super T> disposer) {
    return Streams.bindExec(this, disposer);
  }

  /**
   * Reduces the stream to a single value, by applying the given function successively.
   *
   * @param seed the initial value
   * @param reducer the reducing function
   * @param <R> the type of result
   * @return a promise for the reduced value
   * @since 1.4
   */
  default <R> Promise<R> reduce(R seed, BiFunction<? super R, ? super T, ? extends R> reducer) {
    return Streams.reduce(this, seed, reducer);
  }

  /**
   * Consumes the given publisher eagerly in a forked execution, buffering results until ready to be consumed by this execution.
   *
   * @param execConfig the configuration for the forked execution
   * @param disposer the disposer for any buffered items when the stream errors or is cancelled
   * @return an execution bound publisher that propagates the items of the given publisher
   * @see Streams#fork(Publisher, Action, Action)
   * @since 1.5
   */
  default TransformablePublisher<T> fork(Action<? super ExecSpec> execConfig, Action<? super T> disposer) {
    return Streams.fork(this, execConfig, disposer);
  }

  /**
   * Consumes the given publisher eagerly in a forked execution, buffering results until ready to be consumed by this execution.
   * <p>
   * This method is identical to {@link #fork(Action, Action)}, but uses {@link Action#noop()} for both arguments.
   *
   * @return an execution bound publisher that propagates the items of the given publisher
   * @see Streams#fork(Publisher, Action, Action)
   * @since 1.5
   */
  default TransformablePublisher<T> fork() {
    return Streams.fork(this, Action.noop(), Action.noop());
  }

  /**
   * See {@link Streams#take(long, Publisher)}.
   *
   * @return a publisher that will emit a max of {@code n} elements
   * @since 1.5
   */
  default TransformablePublisher<T> take(long count) {
    return Streams.take(count, this);
  }

  /**
   * See {@link Streams#batch(int, Publisher, Action)}.
   *
   * @return a publisher that batches upstream requests
   * @since 1.5
   */
  default TransformablePublisher<T> batch(int batchSize, Action<? super T> disposer) {
    return Streams.batch(batchSize, this, disposer);
  }

}
