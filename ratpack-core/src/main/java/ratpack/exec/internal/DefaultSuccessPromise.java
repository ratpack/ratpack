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
import ratpack.util.internal.InternalRatpackError;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static ratpack.func.Action.ignoreArg;

public class DefaultSuccessPromise<T> implements SuccessPromise<T> {

  private final Supplier<ExecutionBacking> executionSupplier;
  private final Consumer<? super Fulfiller<? super T>> fulfillment;
  private final Action<? super Throwable> errorHandler;

  public DefaultSuccessPromise(Supplier<ExecutionBacking> executionSupplier, Consumer<? super Fulfiller<? super T>> fulfillment, Action<? super Throwable> errorHandler) {
    this.executionSupplier = executionSupplier;
    this.fulfillment = fulfillment;
    this.errorHandler = errorHandler;
  }

  @Override
  public void then(final Action<? super T> then) {
    final ExecutionBacking executionBacking = executionSupplier.get();
    try {
      doThen(new UserActionFulfiller(executionBacking, then));
    } catch (ExecutionException e) {
      throw e;
    } catch (Exception e) {
      throw new InternalRatpackError("failed to add promise resume action", e);
    }
  }

  private void doThen(final Fulfiller<? super T> fulfiller) {
    fulfillment.accept(fulfiller);
  }

  @Override
  public <O> DefaultPromise<O> map(final Function<? super T, ? extends O> transformer) {
    return new DefaultPromise<>(executionSupplier, downstream -> DefaultSuccessPromise.this.doThen(new Transform<O, O>(downstream, transformer) {
      @Override
      protected void onSuccess(O transformed) {
        downstream.success(transformed);
      }
    }));
  }

  @Override
  public <O> Promise<O> flatMap(final Function<? super T, ? extends Promise<O>> transformer) {
    return new DefaultPromise<>(executionSupplier, downstream -> DefaultSuccessPromise.this.doThen(new Transform<Promise<O>, O>(downstream, transformer) {
      @Override
      protected void onSuccess(Promise<O> transformed) {
        transformed.onError(downstream::error).then(downstream::success);
      }
    }));
  }

  @Override
  public Promise<T> route(final Predicate<? super T> predicate, final Action<? super T> fulfillment) {
    return new DefaultPromise<>(executionSupplier, downstream -> doThen(new Step<T>(downstream) {
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

  private abstract class Step<O> implements Fulfiller<T> {
    protected final Fulfiller<? super O> downstream;

    public Step(Fulfiller<? super O> downstream) {
      this.downstream = downstream;
    }

    @Override
    public void error(Throwable throwable) {
      try {
        errorHandler.execute(throwable);
      } catch (Throwable e) {
        downstream.error(e);
      }
    }
  }

  private class PassThru implements Fulfiller<T> {
    protected final Fulfiller<? super T> downstream;

    public PassThru(Fulfiller<? super T> downstream) {
      this.downstream = downstream;
    }

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
  }

  @Override
  public <O> Promise<O> blockingMap(final Function<? super T, ? extends O> transformer) {
    return flatMap(new Function<T, Promise<O>>() {
      @Override
      public Promise<O> apply(final T t) throws Exception {
        return executionSupplier.get().getExecution().getControl().blocking(() -> transformer.apply(t));
      }
    });
  }

  @Override
  public Promise<T> cache() {
    return new CachingPromise<>(fulfillment, executionSupplier, errorHandler);
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
      ExecutionBacking executionBacking = executionSupplier.get();
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
    return new DefaultPromise<>(executionSupplier, downstream -> doThen(new Step<T>(downstream) {
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
    return throttle.throttle(new DefaultPromise<>(executionSupplier, downstream -> doThen(new PassThru(downstream))));
  }

  private abstract class Transform<I, O> extends Step<O> {
    private final Function<? super T, ? extends I> function;

    public Transform(Fulfiller<? super O> downstream, Function<? super T, ? extends I> function) {
      super(downstream);
      this.function = function;
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

    protected abstract void onSuccess(I transformed);
  }

  private class UserActionFulfiller implements Fulfiller<T> {
    private final ExecutionBacking executionBacking;
    private final Action<? super T> then;

    public UserActionFulfiller(ExecutionBacking executionBacking, Action<? super T> then) {
      this.executionBacking = executionBacking;
      this.then = then;
    }

    @Override
    public void error(final Throwable throwable) {
      try {
        errorHandler.execute(throwable);
      } catch (Throwable errorHandlerThrown) {
        executionBacking.streamSubscribe(h -> h.complete(() -> {
          throw errorHandlerThrown;
        }));
      }
    }

    @Override
    public void success(final T value) {
      try {
        then.execute(value);
      } catch (Throwable throwable) {
        executionBacking.streamSubscribe(h -> h.complete(() -> {
          throw throwable;
        }));
      }
    }
  }

}
