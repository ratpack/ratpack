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
import ratpack.func.Action;
import ratpack.func.Factory;
import ratpack.func.Function;
import ratpack.util.ExceptionUtils;
import ratpack.util.internal.InternalRatpackError;

import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultSuccessPromise<T> implements SuccessPromise<T> {

  private final Factory<ExecutionBacking> executionProvider;
  private final Action<? super Fulfiller<T>> action;
  private final Action<? super Throwable> errorHandler;
  private final static Logger LOGGER = LoggerFactory.getLogger(DefaultSuccessPromise.class);

  public DefaultSuccessPromise(Factory<ExecutionBacking> executionProvider, Action<? super Fulfiller<T>> action, Action<? super Throwable> errorHandler) {
    this.executionProvider = executionProvider;
    this.action = action;
    this.errorHandler = errorHandler;
  }

  @Override
  public void then(final Action<? super T> then) {
    final ExecutionBacking executionBacking = executionProvider.create();
    try {
      executionBacking.continueVia(new Runnable() {
        public void run() {
          doThen(new UserActionFulfiller(executionBacking, then));
        }
      });
    } catch (ExecutionException e) {
      throw e;
    } catch (Exception e) {
      throw new InternalRatpackError("failed to add promise resume action", e);
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

          executionBacking.join(new Action<Execution>() {
            @Override
            public void execute(Execution execution) throws Exception {
              outer.error(throwable);
            }
          });
        }

        @Override
        public void success(final T value) {
          if (!fulfilled.compareAndSet(false, true)) {
            LOGGER.error("", new OverlappingExecutionException("promise already fulfilled"));
            return;
          }

          executionBacking.join(new Action<Execution>() {
            @Override
            public void execute(Execution execution) throws Exception {
              outer.success(value);

            }
          });
        }
      });
    } catch (final Throwable e) {
      if (!fulfilled.compareAndSet(false, true)) {
        LOGGER.error("", new OverlappingExecutionException("exception thrown after promise was fulfilled", e));
      } else {
        executionBacking.join(new ThrowAction(e));
      }
    }
  }

  @Override
  public <O> DefaultPromise<O> map(final Function<? super T, ? extends O> function) {
    return new DefaultPromise<>(executionProvider, new Action<Fulfiller<O>>() {
      @Override
      public void execute(final Fulfiller<O> downstream) throws Exception {
        DefaultSuccessPromise.this.doThen(new Transform<O>(downstream, function) {
          @Override
          protected void onSuccess(O transformed) {
            downstream.success(transformed);
          }
        });
      }
    });
  }

  @Override
  public <O> Promise<O> flatMap(final Function<? super T, ? extends Promise<O>> function) {
    return new DefaultPromise<>(executionProvider, new Action<Fulfiller<O>>() {
      @Override
      public void execute(final Fulfiller<O> downstream) throws Exception {
        DefaultSuccessPromise.this.doThen(new Transform<Promise<O>>(downstream, function) {
          @Override
          protected void onSuccess(Promise<O> transformed) {
            transformed.onError(new Action<Throwable>() {
              @Override
              public void execute(Throwable throwable) throws Exception {
                downstream.error(throwable);
              }
            }).then(new Action<O>() {
              @Override
              public void execute(O o) throws Exception {
                downstream.success(o);
              }
            });
          }
        });
      }
    });
  }

  private static class ThrowAction implements Action<Execution> {
    private final Throwable e;

    public ThrowAction(Throwable e) {
      this.e = e;
    }

    @Override
    public void execute(Execution execution) throws Exception {
      throw ExceptionUtils.toException(e);
    }
  }

  private abstract class Transform<O> implements Fulfiller<T> {
    private final Fulfiller<?> downstream;
    private final Function<? super T, ? extends O> function;

    public Transform(Fulfiller<?> downstream, Function<? super T, ? extends O> function) {
      this.downstream = downstream;
      this.function = function;
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
      O transformed;
      try {
        transformed = function.apply(value);
      } catch (Throwable e) {
        downstream.error(e);
        return;
      }

      onSuccess(transformed);
    }

    protected abstract void onSuccess(O transformed);
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
      } catch (Exception e) {
        executionBacking.join(new ThrowAction(e));
      }
    }

    @Override
    public void success(final T value) {
      try {
        then.execute(value);
      } catch (Exception e) {
        executionBacking.join(new ThrowAction(e));
      }
    }
  }
}
