/*
 * Copyright 2013 the original author or authors.
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

package ratpack.background.internal;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import ratpack.func.Action;
import ratpack.handling.Background;
import ratpack.handling.BackgroundInterceptor;
import ratpack.handling.Context;
import ratpack.promise.SuccessOrErrorPromise;
import ratpack.promise.SuccessPromise;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import static ratpack.util.ExceptionUtils.toException;

public class DefaultBackground implements Background {

  private final ExecutorService foregroundExecutor;
  private final ListeningExecutorService backgroundExecutor;
  private final ThreadLocal<Context> contextThreadLocal;

  public DefaultBackground(ExecutorService foregroundExecutor, ListeningExecutorService backgroundExecutor, ThreadLocal<Context> contextThreadLocal) {
    this.foregroundExecutor = foregroundExecutor;
    this.backgroundExecutor = backgroundExecutor;
    this.contextThreadLocal = contextThreadLocal;
  }

  @Override
  public <T> SuccessOrErrorPromise<T> exec(Callable<T> operation) {
    return new DefaultSuccessOrErrorPromise<>(operation, contextThreadLocal.get());
  }

  private class DefaultSuccessOrErrorPromise<T> implements SuccessOrErrorPromise<T> {
    private final Callable<T> backgroundAction;
    protected final Context context;

    DefaultSuccessOrErrorPromise(Callable<T> backgroundAction, Context context) {
      this.backgroundAction = backgroundAction;
      this.context = context;
    }

    @Override
    public SuccessPromise<T> onError(final Action<? super Throwable> errorHandler) {
      return new DefaultSuccessPromise<>(context, backgroundAction, new Action<Throwable>() {
        @Override
        public void execute(Throwable t) {
          try {
            errorHandler.execute(t);
          } catch (Throwable errorHandlerError) {
            new ForwardToContextErrorHandler().execute(errorHandlerError);
          }
        }
      });
    }

    @Override
    public void then(Action<? super T> then) {
      new DefaultSuccessPromise<>(context, backgroundAction, new ForwardToContextErrorHandler()).then(then);
    }

    class ForwardToContextErrorHandler implements Action<Throwable> {
      @Override
      public void execute(Throwable t) {
        context.error(toException(t));
      }
    }
  }

  private class DefaultSuccessPromise<T> implements SuccessPromise<T> {

    private final Context context;
    private final Callable<T> backgroundAction;
    private final Action<Throwable> errorHandler;

    DefaultSuccessPromise(Context context, Callable<T> backgroundAction, Action<Throwable> errorHandler) {
      this.context = context;
      this.backgroundAction = backgroundAction;
      this.errorHandler = errorHandler;
    }

    @Override
    public void then(final Action<? super T> then) {
      final List<BackgroundInterceptor> interceptors = context.getAll(BackgroundInterceptor.class);

      final ListenableFuture<T> future = backgroundExecutor.submit(new Callable<T>() {

        private Exception exception;
        private T result;
        private int i;

        @Override
        public T call() throws Exception {
          contextThreadLocal.set(context);
          try {
            if (interceptors.isEmpty()) {
              return backgroundAction.call();
            } else {
              nextInterceptor();
              if (exception != null) {
                throw exception;
              } else {
                return result;
              }
            }
          } finally {
            contextThreadLocal.remove();
          }
        }

        private void nextInterceptor() {
          if (i < interceptors.size()) {
            BackgroundInterceptor interceptor = interceptors.get(i++);
            Runnable continuation = new Runnable() {
              @Override
              public void run() {
                try {
                  nextInterceptor();
                } catch (Exception ignore) {
                  // do nothing
                }
              }
            };
            interceptor.toBackground(context, continuation);
          } else {
            try {
              result = backgroundAction.call();
            } catch (Exception e) {
              exception = e;
            }
          }
        }
      });

      Futures.addCallback(future, new FutureCallback<T>() {

        private Exception exception;
        int i = interceptors.size() - 1;

        @Override
        public void onSuccess(final T result) {
          contextThreadLocal.set(context);
          try {
            if (interceptors.isEmpty()) {
              then.execute(result);
            } else {
              nextInterceptor(new Runnable() {
                @Override
                public void run() {
                  try {
                    then.execute(result);
                  } catch (Exception e) {
                    exception = e;
                  }
                }
              });
              if (exception != null) {
                throw exception;
              }
            }
          } catch (Exception e) {
            context.error(e);
          } finally {
            contextThreadLocal.remove();
          }
        }

        @Override
        public void onFailure(final Throwable t) {
          contextThreadLocal.set(context);
          try {
            if (interceptors.isEmpty()) {
              errorHandler.execute(t);
            } else {
              nextInterceptor(new Runnable() {
                @Override
                public void run() {
                  try {
                    errorHandler.execute(t);
                  } catch (Exception e) {
                    exception = e;
                  }
                }
              });
              if (exception != null) {
                throw exception;
              }
            }
          } catch (Exception e) {
            context.error(e);
          } finally {
            contextThreadLocal.remove();
          }
        }

        private void nextInterceptor(final Runnable action) {
          if (i >= 0) {
            BackgroundInterceptor interceptor = interceptors.get(i--);
            Runnable continuation = new Runnable() {
              @Override
              public void run() {
                try {
                  nextInterceptor(action);
                } catch (Exception ignore) {
                  // do nothing
                }
              }
            };
            interceptor.toForeground(context, continuation);
          } else {
            action.run();
          }
        }

      }, foregroundExecutor);
    }
  }
}
