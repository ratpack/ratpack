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
import ratpack.handling.Context;
import ratpack.handling.ProcessingInterceptor;
import ratpack.handling.internal.FinishedOnThreadCallbackManager;
import ratpack.handling.internal.InterceptedOperation;
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
    private FinishedOnThreadCallbackManager finishedOnThreadCallbackManager;

    DefaultSuccessPromise(Context context, Callable<T> backgroundAction, Action<Throwable> errorHandler) {
      this.context = context;
      this.backgroundAction = backgroundAction;
      this.errorHandler = errorHandler;
      this.finishedOnThreadCallbackManager = context.get(FinishedOnThreadCallbackManager.class);
    }

    @Override
    public void then(final Action<? super T> then) {
      finishedOnThreadCallbackManager.register(new Runnable() {
        @Override
        public void run() {
          List<ProcessingInterceptor> interceptors = context.getAll(ProcessingInterceptor.class);
          ListenableFuture<T> future = backgroundExecutor.submit(new BackgroundOperation(interceptors));
          Futures.addCallback(future, new ForegroundResume(interceptors, then), foregroundExecutor);
        }
      });
    }

    private class BackgroundOperation extends InterceptedOperation implements java.util.concurrent.Callable<T> {

      private Exception exception;
      private T result;

      public BackgroundOperation(List<ProcessingInterceptor> interceptors) {
        super(ProcessingInterceptor.Type.BACKGROUND, interceptors, context);
      }

      @Override
      public T call() throws Exception {
        contextThreadLocal.set(context);
        try {
          run();
          if (exception != null) {
            throw exception;
          } else {
            return result;
          }
        } finally {
          contextThreadLocal.remove();
        }
      }

      @Override
      protected void performOperation() {
        try {
          result = backgroundAction.call();
        } catch (Exception e) {
          exception = e;
        }
      }
    }

    private class ForegroundResume implements FutureCallback<T> {

      private final List<ProcessingInterceptor> interceptors;
      private final Action<? super T> then;
      private Exception exception;

      public ForegroundResume(List<ProcessingInterceptor> interceptors, Action<? super T> then) {
        this.interceptors = interceptors;
        this.then = then;
      }

      @Override
      public void onSuccess(final T result) {
        contextThreadLocal.set(context);
        try {
          new InterceptedOperation(ProcessingInterceptor.Type.FOREGROUND, interceptors, context) {
            @Override
            protected void performOperation() {
              try {
                then.execute(result);
                finishedOnThreadCallbackManager.fire();
              } catch (Exception e) {
                exception = e;
              }
            }
          }.run();
          if (exception != null) {
            throw exception;
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
          new InterceptedOperation(ProcessingInterceptor.Type.FOREGROUND, interceptors, context) {
            @Override
            protected void performOperation() {
              try {
                errorHandler.execute(t);
                finishedOnThreadCallbackManager.fire();
              } catch (Exception e) {
                exception = e;
              }
            }
          }.run();
          if (exception != null) {
            throw exception;
          }
        } catch (Exception e) {
          context.error(e);
        } finally {
          contextThreadLocal.remove();
        }
      }
    }
  }

}
