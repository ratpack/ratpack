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

package ratpack.exec;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import ratpack.api.NonBlocking;
import ratpack.func.Action;

import java.util.concurrent.CompletableFuture;

/**
 * A fulfiller of an asynchronous promise.
 * <p>
 * This type is used to integrate with asynchronous APIs, via the {@link Promise#of(Action)} method.
 *
 * @param <T> the type of value that was promised.
 * @see Promise#of(Action)
 * @see Promise
 */
public interface Fulfiller<T> {

  /**
   * Fulfills the promise with an error result.
   *
   * @param throwable the error result
   */
  @NonBlocking
  void error(Throwable throwable);

  /**
   * Fulfills the promise with the given value.
   *
   * @param value the value to provide to the promise subscriber
   */
  @NonBlocking
  void success(T value);

  /**
   * Fulfills via the given result.
   *
   * @param result the result to use to fulfill.
   */
  default void accept(Result<? extends T> result) {
    if (result.isSuccess()) {
      success(result.getValue());
    } else {
      error(result.getThrowable());
    }
  }

  /**
   * Fulfills via the given completable future.
   *
   * @param future the future to use to fulfill
   */
  default void accept(CompletableFuture<? extends T> future) {
    future.handle((value, failure) -> {
      if (failure == null) {
        success(value);
      } else {
        error(failure);
      }

      return null;
    });
  }

  /**
   * Fulfills via the given ListenableFuture.
   *
   * @param listenableFuture the future to use to fulfill
   */
  default void accept(ListenableFuture<? extends T> listenableFuture) {
    Futures.addCallback(listenableFuture, new FutureCallback<T>() {
      @Override
      public void onSuccess(T result) {
        success(result);
      }

      @Override
      public void onFailure(Throwable t) {
        error(t);
      }
    });
  }

}
