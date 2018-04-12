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

import ratpack.exec.*;
import ratpack.func.Action;
import ratpack.func.BiFunction;
import ratpack.func.Block;
import ratpack.func.Function;
import ratpack.util.Exceptions;

import java.time.Duration;

public class DefaultPromise<T> implements Promise<T> {

  public static final Promise<Void> NULL = Promise.value(null);

  private final Upstream<T> upstream;

  public DefaultPromise(Upstream<T> upstream) {
    this.upstream = upstream;
  }

  @Override
  public void then(final Action<? super T> then) {
    ExecThreadBinding.requireComputeThread("Promise.then() can only be called on a compute thread (use Blocking.on() to use a promise on a blocking thread)");
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
    ExecThreadBinding.requireComputeThread("Promise.connect() can only be called on a compute thread (use Blocking.on() to use a promise on a blocking thread)");
    doConnect(downstream);
  }

  private void doConnect(Downstream<? super T> downstream) {
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


  public static <T> void retryAttempt(int attemptNum, int maxAttempts, Upstream<? extends T> up, Downstream<? super T> down, BiFunction<? super Integer, ? super Throwable, Promise<Duration>> onError) throws Exception {
    up.connect(down.onError(e -> {
      if (attemptNum > maxAttempts) {
        down.error(e);
      } else {
        Promise<Duration> delay;
        try {
          delay = onError.apply(attemptNum, e);
        } catch (Throwable errorHandlerError) {
          if (errorHandlerError != e) {
            errorHandlerError.addSuppressed(e);
          }
          down.error(errorHandlerError);
          return;
        }

        delay.connect(new Downstream<Duration>() {
          @Override
          public void success(Duration value) {
            Execution.sleep(value, () ->
              retryAttempt(attemptNum + 1, maxAttempts, up, down, onError)
            );
          }

          @Override
          public void error(Throwable throwable) {
            down.error(throwable);
          }

          @Override
          public void complete() {
            down.complete();
          }
        });
      }
    }));
  }

}
