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

package ratpack.exec.internal;

import ratpack.exec.Downstream;
import ratpack.exec.ExecutionException;
import ratpack.exec.Promise;
import ratpack.exec.Upstream;
import ratpack.func.Action;
import ratpack.func.BiAction;
import ratpack.func.Block;
import ratpack.func.Function;
import ratpack.util.Exceptions;

import java.time.Duration;

public class DefaultPromise<T> implements Promise<T> {

  private final Upstream<T> upstream;

  public DefaultPromise(Upstream<T> upstream) {
    this.upstream = upstream;
  }

  @Override
  public void then(final Action<? super T> then) {
    ThreadBinding.requireComputeThread("Promise.then() can only be called on a compute thread (use Blocking.on() to use a promise on a blocking thread)");
    doConnect(new Downstream<T>() {
      @Override
      public void success(T value) {
        try {
          then.execute(value);
        } catch (Throwable e) {
          throwError(e);
        }
      }

      @Override
      public void error(Throwable throwable) {
        throwError(throwable);
      }

      @Override
      public void complete() {

      }
    });
  }

  @Override
  public void connect(Downstream<? super T> downstream) {
    ThreadBinding.requireComputeThread("Promise.connect() can only be called on a compute thread (use Blocking.on() to use a promise on a blocking thread)");
    doConnect(downstream);
  }

  public void doConnect(Downstream<? super T> downstream) {
    try {
      upstream.connect(downstream);
    } catch (ExecutionException e) {
      throw e;
    } catch (Exception e) {
      throwError(e);
    }
  }

  public static void throwError(Throwable throwable) {
    DefaultExecution.require().delimit(Action.throwException(), h -> h.resume(Block.throwException(throwable)));
  }

  @Override
  public <O> Promise<O> transform(Function<? super Upstream<? extends T>, ? extends Upstream<O>> upstreamTransformer) {
    try {
      return new DefaultPromise<>(upstreamTransformer.apply(upstream));
    } catch (Exception e) {
      throw Exceptions.uncheck(e);
    }
  }

  @Override
  public Promise<T> retry(int maxAttempts, Duration delay, BiAction<? super Integer, ? super Throwable> onError) {
    return transform(up -> down -> retryAttempt(1, maxAttempts, delay, up, down, onError));
  }

  private void retryAttempt(int attemptNum, int maxAttempts, Duration delay, Upstream<? extends T> up, Downstream<? super T> down, BiAction<? super Integer, ? super Throwable> onError) throws Exception {
    up.connect(down.onError(e -> {
      if (attemptNum > maxAttempts) {
        down.error(e);
      } else {
        try {
          onError.execute(attemptNum, e);
        } catch (Throwable errorHandlerError) {
          if (errorHandlerError != e) {
            errorHandlerError.addSuppressed(e);
          }
          down.error(errorHandlerError);
          return;
        }
        Promise.value(null).delay(delay).then(v -> retryAttempt(attemptNum + 1, maxAttempts, delay, up, down, onError));
      }
    }));
  }

}
