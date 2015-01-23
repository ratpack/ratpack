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

/**
 * A wrapper over a {@link Publisher} that makes it more convenient to chain transformations of different kinds.
 * <p>
 * Note that this type implements the publisher interface, so behaves just like the publisher that it is wrapping with respect to the {@link Publisher#subscribe(org.reactivestreams.Subscriber)} method.
 *
 * @param <T> the type of item emitted by this publisher
 */
public interface TransformablePublisher<T> extends Publisher<T> {

  /**
   * See {@link ratpack.stream.Streams#map(Publisher, Function)}.
   *
   * @param function the transformation
   * @param <O> the type of transformed item
   * @return the transformed publisher
   */
  default <O> TransformablePublisher<O> map(Function<? super T, ? extends O> function) {
    return Streams.map(this, function);
  }

  /**
   * See {@link ratpack.stream.Streams#flatMap(Publisher, Function)}.
   *
   * @param function the transformation
   * @param <O> the type of transformed item
   * @return the transformed publisher
   */
  default <O> TransformablePublisher<O> flatMap(Function<? super T, ? extends Promise<? extends O>> function) {
    return Streams.flatMap(this, function);
  }

  /**
   * See {@link ratpack.stream.Streams#buffer(Publisher)}.
   *
   * @return a buffering publisher
   */
  default TransformablePublisher<T> buffer() {
    return Streams.buffer(this);
  }

  /**
   * See {@link ratpack.stream.Streams#gate(Publisher, Action)}.
   *
   * @param valveReceiver an action that receives a runnable “valve” that when run allows request to start flowing upstream
   * @return a publisher that is logically equivalent to the given publisher as far as subscribers are concerned
   */
  default TransformablePublisher<T> gate(Action<? super Runnable> valveReceiver) {
    return Streams.gate(this, valveReceiver);
  }

  /**
   * See {@link ratpack.stream.Streams#wiretap(Publisher, Action)}.
   *
   * @param listener the listener for emitted items
   * @return a publisher that is logically equivalent to the given publisher as far as subscribers are concerned
   */
  default TransformablePublisher<T> wiretap(Action<? super StreamEvent<? super T>> listener) {
    return Streams.wiretap(this, listener);
  }

  /**
   * See {@link ratpack.stream.Streams#multicast(Publisher)}.
   *
   * @return a publisher that respects back pressure for each of its subscribers
   */
  default TransformablePublisher<T> multicast() {
    return Streams.multicast(this);
  }

  /**
   * See {@link ratpack.stream.Streams#toPromise(Publisher)}.
   *
   * @return a promise for this publisher's single item
   */
  default Promise<T> toPromise() {
    return Streams.toPromise(this);
  }

  /**
   * See {@link ratpack.stream.Streams#toPromise(ExecControl, Publisher)}.
   *
   * @param execControl the exec control to create the promise from
   * @return a promise for this publisher's single item
   */
  default Promise<T> toPromise(ExecControl execControl) {
    return Streams.toPromise(execControl, this);
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
  default <O> TransformablePublisher<O> transform(java.util.function.Function<? super TransformablePublisher<T>, ? extends Publisher<O>> transformer) {
    return Streams.transformable(transformer.apply(this));
  }

  /**
   * See {@link ratpack.stream.Streams#streamMap(org.reactivestreams.Publisher, ratpack.func.Function)}.
   *
   * @param function the transformation
   * @param <O> the type of transformed item
   * @return the transformed publisher
   */
  default <O> TransformablePublisher<O> streamMap(Function<? super WriteStream<O>, ? extends WriteStream<T>> function) {
    return Streams.streamMap(this, function);
  }

}
