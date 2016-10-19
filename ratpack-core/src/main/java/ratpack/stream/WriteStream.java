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
import org.reactivestreams.Subscription;
import ratpack.func.Action;

/**
 * The write end of a data stream.
 * <p>
 * Users of a write stream must not call any of the methods of this interface concurrently.
 * That is, write streams are not thread safe.
 *
 * @param <T> the type of item emitted.
 * @see Streams#streamMap(Publisher, StreamMapper)
 */
public interface WriteStream<T> {

  /**
   * Emit an item.
   *
   * @param item the item to emit
   */
  void item(T item);

  /**
   * Signals a stream error.
   * <p>
   * No other methods should be called on this stream after calling this method.
   * It is not necessary to enforce this on user provided implementations of write streams as this is managed internally.
   *
   * @param throwable the error
   */
  void error(Throwable throwable);

  /**
   * Signals that the stream has completed and that no more items (or errors) are to come.
   * <p>
   * No other methods should be called on this stream after calling this method.
   * It is not necessary to enforce this on user provided implementations of write streams as this is managed internally.
   */
  void complete();

  /**
   * Creates a new write stream that passes error and complete signals on to this stream, but passes items to the given action.
   * <p>
   * This effectively creates an <i>upstream</i> write stream that transforms items.
   * It is often useful when {@link Streams#streamMap(Publisher, StreamMapper)}  mapping streams}.
   * <p>
   * The {@code itemMapper} typically manually calls {@link #item(Object)} on this stream, one or more times, when receiving an item.
   * That is, the action may emit multiple items downstream in a particular invocation.
   * If the mapper throws an exception, the exception will be emitted via {@link #error(Throwable)} and the subscription will be cancelled.
   * The mapper may call {@link #complete()} or {@link #error(Throwable)}, but should ensure that it does not call any other methods of this interface after.
   *
   * @param subscription the upstream subscription
   * @param itemMapper the item mapper
   * @param <O> the type of item received by the returned write stream
   * @return a write stream that writes through to this write stream
   * @since 1.4
   */
  default <O> WriteStream<O> itemMap(Subscription subscription, Action<? super O> itemMapper) {
    return new WriteStream<O>() {
      @Override
      public void item(O item) {
        try {
          itemMapper.execute(item);
        } catch (Exception e) {
          subscription.cancel();
          error(e);
        }
      }

      @Override
      public void error(Throwable throwable) {
        WriteStream.this.error(throwable);
      }

      @Override
      public void complete() {
        WriteStream.this.complete();
      }
    };
  }

  /**
   * @deprecated since 1.4, use {@link #itemMap(Subscription, Action)}
   */
  @Deprecated
  default <O> WriteStream<O> itemMap(Action<? super O> itemMapper) {
    return new WriteStream<O>() {
      @Override
      public void item(O item) {
        try {
          itemMapper.execute(item);
        } catch (Exception e) {
          error(e);
        }
      }

      @Override
      public void error(Throwable throwable) {
        WriteStream.this.error(throwable);
      }

      @Override
      public void complete() {
        WriteStream.this.complete();
      }
    };
  }
}
