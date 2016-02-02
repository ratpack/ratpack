/*
 * Copyright 2015 the original author or authors.
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
import ratpack.func.Action;
import ratpack.func.Block;

import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletionStage;

/**
 * A consumer of a single asynchronous value.
 * <p>
 * Downstreams {@link Upstream#connect(Downstream) connect} to {@link Upstream upstreams}.
 * Once connected, an upstream will invoke only one of either the {@link #success}, {@link #error} or {@link #complete} methods exactly once.
 *
 * @param <T> the type of value emitted downstream
 * @see Promise#transform(ratpack.func.Function)
 */
public interface Downstream<T> {

  /**
   * Signals the successful production of the upstream value.
   *
   * @param value the upstream value
   */
  void success(T value);

  /**
   * Signals the unsuccessful production of the upstream value.
   *
   * @param throwable what went wrong
   */
  void error(Throwable throwable);

  /**
   * Signals that the upstream will not be providing a value, as it has terminated.
   */
  void complete();

  /**
   * Wrap this downstream, using the given action as the implementation of the {@link #success} method.
   * <p>
   * All {@link #error} and {@link #complete} signals will be forwarded to {@code this} downstream,
   * and the given action called with the value if {@link #success} is signalled.
   *
   * @param action the implementation of the success signal receiver for the returned downstream
   * @param <O> the type of item accepted
   * @return a downstream
   */
  default <O> Downstream<O> onSuccess(Action<? super O> action) {
    return new Downstream<O>() {
      @Override
      public void success(O value) {
        try {
          action.execute(value);
        } catch (Throwable e) {
          Downstream.this.error(e);
        }
      }

      @Override
      public void error(Throwable throwable) {
        Downstream.this.error(throwable);
      }

      @Override
      public void complete() {
        Downstream.this.complete();
      }
    };
  }

  /**
   * Wrap this downstream, using the given action as the implementation of the {@link #error(Throwable)} method.
   * <p>
   * All {@link #success} and {@link #complete} signals will be forwarded to {@code this} downstream,
   * and the given action called with the value if {@link #success} is signalled.
   *
   * @param action the implementation of the error signal receiver for the returned downstream
   * @return a downstream
   */
  default Downstream<T> onError(Action<? super Throwable> action) {
    return new Downstream<T>() {
      @Override
      public void success(T value) {
        Downstream.this.success(value);
      }

      @Override
      public void error(Throwable throwable) {
        try {
          action.execute(throwable);
        } catch (Exception e) {
          e.addSuppressed(throwable);
          Downstream.this.error(e);
        }
      }

      @Override
      public void complete() {
        Downstream.this.complete();
      }
    };
  }

  /**
   * Wrap this downstream, using the given action as the implementation of the {@link #complete()} method.
   * <p>
   * All {@link #success} and {@link #error} signals will be forwarded to {@code this} downstream,
   * and the given action called with the value if {@link #complete} is signalled.
   *
   * @param block the implementation of the complete signal receiver for the returned downstream
   * @return a downstream
   */
  default Downstream<T> onComplete(Block block) {
    return new Downstream<T>() {
      @Override
      public void success(T value) {
        Downstream.this.success(value);
      }

      @Override
      public void error(Throwable throwable) {
        Downstream.this.error(throwable);
      }

      @Override
      public void complete() {
        try {
          block.execute();
        } catch (Exception e) {
          Downstream.this.error(e);
        }
      }
    };
  }

  /**
   * Signals this downstream, based on the given result.
   *
   * @param result the result to signal
   */
  default void accept(ExecResult<? extends T> result) {
    if (result.isComplete()) {
      complete();
    } else if (result.isError()) {
      error(result.getThrowable());
    } else {
      success(result.getValue());
    }
  }

  /**
   * Signals this downstream, based on the given result.
   *
   * @param result the result to signal
   */
  default void accept(Result<? extends T> result) {
    if (result.isError()) {
      error(result.getThrowable());
    } else {
      success(result.getValue());
    }
  }

  /**
   * Sends the result of a CompletionStage downstream.
   *
   * @param completionStage the CompletionStage to consume the value of
   */
  default void accept(CompletionStage<? extends T> completionStage) {
    completionStage.handleAsync((value, failure) -> {
      if (failure == null) {
        success(value);
      } else {
        error(failure);
      }

      return null;
    }, Execution.current().getEventLoop());
  }

  /**
   * Sends the result of the future downstream.
   *
   * @param future the future to consume the value of
   */
  default void accept(ListenableFuture<? extends T> future) {
    Futures.addCallback(future, new FutureCallback<T>() {
      @Override
      public void onSuccess(T result) {
        success(result);
      }

      @Override
      public void onFailure(Throwable t) {
        error(t);
      }
    }, Execution.current().getEventLoop());
  }

  /**
   * Creates a JDK {@link CompletionHandler} that connects to this downstream.
   *
   * @return a completion handler
   * @since 1.2
   */
  default <I extends T, A> CompletionHandler<I, A> completionHandler() {
    return new CompletionHandler<I, A>() {
      @Override
      public void completed(I result, A attachment) {
        success(result);
      }

      @Override
      public void failed(Throwable exc, A attachment) {
        error(exc);
      }
    };
  }

}
