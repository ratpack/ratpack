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
import ratpack.background.Background;
import ratpack.handling.Context;
import ratpack.util.Action;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import static ratpack.util.ExceptionUtils.toException;

public class DefaultBackground implements Background {

  private final ExecutorService mainExecutor;
  private final ListeningExecutorService backgroundExecutor;
  private final Context context;
  private final ThreadLocal<Context> contextThreadLocal;

  public DefaultBackground(ExecutorService mainExecutor, ListeningExecutorService backgroundExecutor, Context context, ThreadLocal<Context> contextThreadLocal) {
    this.mainExecutor = mainExecutor;
    this.backgroundExecutor = backgroundExecutor;
    this.context = context;
    this.contextThreadLocal = contextThreadLocal;
  }

  @Override
  public <T> SuccessOrError<T> exec(Callable<T> operation) {
    return new DefaultSuccessOrError<>(operation);
  }

  private class DefaultSuccessOrError<T> implements SuccessOrError<T> {

    private final Callable<T> backgroundAction;

    DefaultSuccessOrError(Callable<T> backgroundAction) {
      this.backgroundAction = backgroundAction;
    }

    @Override
    public Success<T> onError(final Action<? super Throwable> errorHandler) {
      return new DefaultSuccess<>(backgroundAction, new Action<Throwable>() {
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
      new DefaultSuccess<>(backgroundAction, new ForwardToContextErrorHandler()).then(then);
    }

    class ForwardToContextErrorHandler implements Action<Throwable> {
      @Override
      public void execute(Throwable t) {
        context.error(toException(t));
      }
    }
  }

  private class DefaultSuccess<T> implements Success<T> {

    private final Callable<T> backgroundAction;
    private final Action<Throwable> errorHandler;

    DefaultSuccess(Callable<T> backgroundAction, Action<Throwable> errorHandler) {
      this.backgroundAction = backgroundAction;
      this.errorHandler = errorHandler;
    }

    @Override
    public void then(final Action<? super T> then) {
      final ListenableFuture<T> future = backgroundExecutor.submit(new Callable<T>() {
        @Override
        public T call() throws Exception {
          contextThreadLocal.set(context);
          try {
            return backgroundAction.call();
          } finally {
            contextThreadLocal.remove();
          }
        }
      });
      Futures.addCallback(future, new FutureCallback<T>() {
        @Override
        public void onSuccess(T result) {
          contextThreadLocal.set(context);
          try {
            then.execute(result);
          } catch (Exception e) {
            context.error(e);
          } finally {
            contextThreadLocal.remove();
          }
        }

        @Override
        public void onFailure(Throwable t) {
          contextThreadLocal.set(context);
          try {
            errorHandler.execute(t);
          } catch (Exception e) {
            context.error(e);
          } finally {
            contextThreadLocal.remove();
          }
        }
      }, mainExecutor);
    }
  }
}
