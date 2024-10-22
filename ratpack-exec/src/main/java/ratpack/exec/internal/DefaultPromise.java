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
import ratpack.exec.util.retry.RetryPolicy;
import ratpack.func.*;

import java.time.Duration;

public class DefaultPromise<T> implements Promise<T> {

  public static final Promise<Void> NULL = Promise.value(null);

  private final Upstream<T> upstream;

  public static <T> Promise<T> of(Upstream<T> upstream) {
    if (upstream instanceof Promise) {
      return (Promise<T>) upstream;
    } else {
      return new DefaultPromise<>(upstream);
    }
  }

  private DefaultPromise(Upstream<T> upstream) {
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
          DefaultExecution.throwError(e);
        }
      }

      @Override
      public void error(Throwable throwable) {
        DefaultExecution.throwError(throwable);
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
    } catch (Throwable e) {
      DefaultExecution.throwError(e);
    }
  }

  @Override
  public <O> Promise<O> transform(Function<? super Upstream<? extends T>, ? extends Upstream<O>> upstreamTransformer) {
    Upstream<O> transformedUpstream;
    try {
      transformedUpstream = upstreamTransformer.apply(upstream);
    } catch (Throwable e) {
      return Promise.error(e);
    }
    return new DefaultPromise<>(transformedUpstream);
  }

  @Override
  public Operation operation() {
    if (upstream instanceof Operation) {
      return (Operation) upstream;
    } else {
      return Promise.super.operation();
    }
  }

  public static <T> void retryAttempt(int attemptNum, int maxAttempts, Upstream<? extends T> up, Downstream<? super T> down, BiFunction<? super Integer, ? super Throwable, Promise<Duration>> onError) {
    up.connect(down.onError(e -> {
      if (attemptNum > maxAttempts) {
        down.error(e);
      } else {
        Promise<Duration> delay;
        try {
          delay = onError.apply(attemptNum, e);
        } catch (Throwable errorHandlerError) {
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

  public static <T> void retry(Predicate<? super Throwable> predicate, RetryPolicy retryPolicy, Upstream<? extends T> up, Downstream<? super T> down, BiAction<? super Integer, ? super Throwable> onError) {
    up.connect(down.onError(e -> {
      if (predicate.apply(e)) {
        if (retryPolicy.isExhausted()) {
          down.error(e);
        } else {
          Promise<Duration> delay;
          try {
            onError.execute(retryPolicy.attempts(), e);
            delay = retryPolicy.delay();
          } catch (Throwable errorHandlerError) {
            down.error(errorHandlerError);
            return;
          }

          delay.connect(new Downstream<Duration>() {
            @Override
            public void success(Duration value) {
              Execution.sleep(value, () ->
                retry(predicate, retryPolicy.increaseAttempt(), up, down, onError)
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
      } else {
        down.error(e);
      }
    }));
  }

}
