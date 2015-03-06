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
import ratpack.func.Function;
import ratpack.func.NoArgAction;
import ratpack.func.Predicate;
import ratpack.util.ExceptionUtils;
import ratpack.util.internal.InternalRatpackError;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static ratpack.exec.ExecControl.execControl;
import static ratpack.func.Action.ignoreArg;

public class DefaultPromise<T> implements Promise<T> {

  private final Supplier<ExecutionBacking> executionSupplier;
  private final Consumer<? super Fulfiller<T>> fulfillment;

  public DefaultPromise(Supplier<ExecutionBacking> executionSupplier, Consumer<? super Fulfiller<T>> fulfillment) {
    this.executionSupplier = executionSupplier;
    this.fulfillment = fulfillment;
  }

  @Override
  public void then(final Action<? super T> then) {
    try {
      doThen(new Fulfiller<T>() {
        @Override
        public void error(final Throwable throwable) {
          throwError(throwable);
        }

        @Override
        public void success(final T value) {
          try {
            then.execute(value);
          } catch (Throwable throwable) {
            throwError(throwable);
          }
        }
      });
    } catch (ExecutionException e) {
      throw e;
    } catch (Exception e) {
      throw new InternalRatpackError("failed to add promise resume action", e);
    }
  }

  private void throwError(Throwable throwable) {
    getExecution().streamSubscribe(h ->
        h.complete(() -> {
          throw ExceptionUtils.toException(throwable);
        })
    );
  }

  private ExecutionBacking getExecution() {
    return executionSupplier.get();
  }

  @Override
  public Promise<T> onError(Action<? super Throwable> errorHandler) {
    return new DefaultPromise<>(executionSupplier, downstream -> doThen(new Fulfiller<T>() {
      @Override
      public void error(Throwable throwable) {
        try {
          errorHandler.execute(throwable);
        } catch (Throwable e) {
          downstream.error(e);
        }
      }

      @Override
      public void success(T value) {
        downstream.success(value);
      }
    }));

  }

  private void doThen(final Fulfiller<T> fulfiller) {
    fulfillment.accept(fulfiller);
  }

  @Override
  public <O> DefaultPromise<O> map(final Function<? super T, ? extends O> transformer) {
    return new DefaultPromise<>(executionSupplier, downstream -> doThen(new Transform<>(downstream, transformer, downstream::success)));
  }

  @Override
  public <O> Promise<O> apply(Function<? super Promise<T>, ? extends Promise<O>> function) {
    try {
      return function.apply(this);
    } catch (Exception e) {
      return execControl().failedPromise(e);
    }
  }

  @Override
  public <O> O to(Function<? super Promise<T>, ? extends O> function) throws Exception {
    return function.apply(this);
  }

  @Override
  public <O> Promise<O> flatMap(final Function<? super T, ? extends Promise<O>> transformer) {
    return new DefaultPromise<>(executionSupplier, downstream -> doThen(new Transform<>(downstream, transformer, transformed -> transformed.onError(downstream::error).then(downstream::success))));
  }

  @Override
  public Promise<T> route(final Predicate<? super T> predicate, final Action<? super T> fulfillment) {
    return new DefaultPromise<>(executionSupplier, downstream -> doThen(new Operation<T>(downstream) {
      @Override
      public void success(T value) {
        boolean apply;
        try {
          apply = predicate.apply(value);
        } catch (Throwable e) {
          error(e);
          return;
        }

        if (apply) {
          try {
            fulfillment.execute(value);
          } catch (Throwable e) {
            error(e);
          }
        } else {
          downstream.success(value);
        }
      }
    }));
  }

  @Override
  public Promise<T> onNull(final NoArgAction onNull) {
    return route(Objects::isNull, ignoreArg(onNull));
  }

  private abstract class Operation<O> implements Fulfiller<T> {
    protected final Fulfiller<? super O> downstream;

    public Operation(Fulfiller<? super O> downstream) {
      this.downstream = downstream;
    }

    @Override
    public void error(Throwable throwable) {
      downstream.error(throwable);
    }
  }

  @Override
  public <O> Promise<O> blockingMap(final Function<? super T, ? extends O> transformer) {
    return flatMap(t -> getExecution().getExecution().getControl().blocking(() -> transformer.apply(t)));
  }

  @Override
  public Promise<T> cache() {
    Consumer<Fulfiller<T>> cachingFulfillment = new CachingFulfillment<>(fulfillment, executionSupplier);
    return new DefaultPromise<>(executionSupplier, cachingFulfillment);
  }

  @Override
  public Promise<T> onYield(Runnable onYield) {
    return new DefaultPromise<>(executionSupplier, downstream -> {
      try {
        onYield.run();
      } catch (Throwable e) {
        downstream.error(e);
        return;
      }
      fulfillment.accept(downstream);
    });
  }

  @Override
  public Promise<T> defer(Action<? super Runnable> releaser) {
    return new DefaultPromise<>(executionSupplier, downstream -> {
      ExecutionBacking executionBacking = getExecution();
      executionBacking.streamSubscribe((streamHandle) -> {
        try {
          releaser.execute((Runnable) () ->
              streamHandle.complete(() -> fulfillment.accept(downstream))
          );
        } catch (Throwable t) {
          downstream.error(t);
        }
      });
    });
  }

  @Override
  public Promise<T> wiretap(Action<? super Result<T>> listener) {
    return new DefaultPromise<>(executionSupplier, downstream -> doThen(new Operation<T>(downstream) {
      @Override
      public void success(T value) {
        try {
          listener.execute(Result.success(value));
        } catch (Throwable t) {
          error(t);
          return;
        }

        downstream.success(value);
      }

      @Override
      public void error(Throwable throwable) {
        try {
          listener.execute(Result.<T>failure(throwable));
        } catch (Throwable t) {
          t.addSuppressed(throwable);
          super.error(t);
          return;
        }

        super.error(throwable);
      }
    }));
  }

  @Override
  public Promise<T> throttled(Throttle throttle) {
    return throttle.throttle(this);
  }

  private class Transform<I, O> extends Operation<O> {
    private final Function<? super T, ? extends I> function;
    private final Consumer<? super I> onSuccess;

    public Transform(Fulfiller<? super O> downstream, Function<? super T, ? extends I> function, Consumer<? super I> onSuccess) {
      super(downstream);
      this.function = function;
      this.onSuccess = onSuccess;
    }

    @Override
    public void success(T value) {
      I transformed;
      try {
        transformed = function.apply(value);
      } catch (Throwable e) {
        downstream.error(e);
        return;
      }

      onSuccess(transformed);
    }

    public void onSuccess(I transformed) {
      onSuccess.accept(transformed);
    }
  }

}
