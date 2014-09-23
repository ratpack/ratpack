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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.*;
import ratpack.func.*;
import ratpack.util.internal.InternalRatpackError;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static ratpack.func.Action.ignoreArg;

public class DefaultSuccessPromise<T> implements SuccessPromise<T> {

  private final static Logger LOGGER = LoggerFactory.getLogger(DefaultSuccessPromise.class);

  private final Factory<ExecutionBacking> executionProvider;
  private final Action<? super Fulfiller<T>> action;
  private final Action<? super Throwable> errorHandler;
  private final AtomicBoolean fired = new AtomicBoolean();

  public DefaultSuccessPromise(Factory<ExecutionBacking> executionProvider, Action<? super Fulfiller<T>> action, Action<? super Throwable> errorHandler) {
    this.executionProvider = executionProvider;
    this.action = action;
    this.errorHandler = errorHandler;
  }

  @Override
  public void then(final Action<? super T> then) {
    if (fired.compareAndSet(false, true)) {
      final ExecutionBacking executionBacking = executionProvider.create();
      try {
        executionBacking.continueVia(() -> doThen(new UserActionFulfiller(executionBacking, then)));
      } catch (ExecutionException e) {
        throw e;
      } catch (Exception e) {
        throw new InternalRatpackError("failed to add promise resume action", e);
      }
    } else {
      throw new MultiplePromiseSubscriptionException();
    }
  }

  private void doThen(final Fulfiller<? super T> outer) {
    final ExecutionBacking executionBacking = executionProvider.create();
    final AtomicBoolean fulfilled = new AtomicBoolean();
    try {
      action.execute(new Fulfiller<T>() {
        @Override
        public void error(final Throwable throwable) {
          if (!fulfilled.compareAndSet(false, true)) {
            LOGGER.error("", new OverlappingExecutionException("promise already fulfilled", throwable));
            return;
          }

          executionBacking.join(execution -> outer.error(throwable));
        }

        @Override
        public void success(final T value) {
          if (!fulfilled.compareAndSet(false, true)) {
            LOGGER.error("", new OverlappingExecutionException("promise already fulfilled"));
            return;
          }

          executionBacking.join(execution -> outer.success(value));
        }
      });
    } catch (final Throwable throwable) {
      if (!fulfilled.compareAndSet(false, true)) {
        LOGGER.error("", new OverlappingExecutionException("exception thrown after promise was fulfilled", throwable));
      } else {
        executionBacking.join(Action.throwException(throwable));
      }
    }
  }

  @Override
  public <O> DefaultPromise<O> map(final Function<? super T, ? extends O> transformer) {
    if (fired.compareAndSet(false, true)) {
      return new DefaultPromise<>(executionProvider, downstream -> DefaultSuccessPromise.this.doThen(new Transform<O, O>(downstream, transformer) {
        @Override
        protected void onSuccess(O transformed) {
          downstream.success(transformed);
        }
      }));
    } else {
      throw new MultiplePromiseSubscriptionException();
    }
  }

  @Override
  public <O> Promise<O> flatMap(final Function<? super T, ? extends Promise<O>> transformer) {
    if (fired.compareAndSet(false, true)) {
      return new DefaultPromise<>(executionProvider, downstream -> DefaultSuccessPromise.this.doThen(new Transform<Promise<O>, O>(downstream, transformer) {
        @Override
        protected void onSuccess(Promise<O> transformed) {
          transformed.onError(downstream::error).then(downstream::success);
        }
      }));
    } else {
      throw new MultiplePromiseSubscriptionException();
    }
  }

  @Override
  public Promise<T> route(final Predicate<? super T> predicate, final Action<? super T> action) {
    if (fired.compareAndSet(false, true)) {
      return new DefaultPromise<>(executionProvider, new Action<Fulfiller<T>>() {
        @Override
        public void execute(final Fulfiller<T> downstream) throws Exception {
          DefaultSuccessPromise.this.doThen(new Step<T>(downstream) {
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
                  action.execute(value);
                } catch (Throwable e) {
                  error(e);
                }
              } else {
                downstream.success(value);
              }
            }
          });
        }
      });
    } else {
      throw new MultiplePromiseSubscriptionException();
    }
  }

  @Override
  public Promise<T> onNull(final NoArgAction onNull) {
    return route(Objects::isNull, ignoreArg(onNull));
  }

  private abstract class Step<O> implements Fulfiller<T> {
    protected final Fulfiller<O> downstream;

    public Step(Fulfiller<O> downstream) {
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

  @Override
  public <O> Promise<O> blockingMap(final Function<? super T, ? extends O> transformer) {
    return flatMap(new Function<T, Promise<O>>() {
      @Override
      public Promise<O> apply(final T t) throws Exception {
        return executionProvider.create().getExecution().getControl().blocking(() -> transformer.apply(t));
      }
    });
  }

  @Override
  public Promise<T> cache() {
    if (fired.compareAndSet(false, true)) {
      return new CachingPromise<>(action, executionProvider, errorHandler);
    } else {
      throw new MultiplePromiseSubscriptionException();
    }
  }

  private abstract class Transform<I, O> extends Step<O> {
    private final Function<? super T, ? extends I> function;

    public Transform(Fulfiller<O> downstream, Function<? super T, ? extends I> function) {
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
        executionBacking.join(Action.throwException(errorHandlerThrown));
      }
    }

    @Override
    public void success(final T value) {
      try {
        then.execute(value);
      } catch (Throwable throwable) {
        executionBacking.join(Action.throwException(throwable));
      }
    }
  }

}
