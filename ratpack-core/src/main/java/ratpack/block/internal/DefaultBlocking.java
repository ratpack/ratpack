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

package ratpack.block.internal;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import ratpack.block.Blocking;
import ratpack.handling.Context;
import ratpack.util.Action;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public class DefaultBlocking implements Blocking {

  private final ExecutorService mainExecutor;
  private final ListeningExecutorService blockingExecutor;
  private final Context context;

  public DefaultBlocking(ExecutorService mainExecutor, ListeningExecutorService blockingExecutor, Context context) {
    this.mainExecutor = mainExecutor;
    this.blockingExecutor = blockingExecutor;
    this.context = context;
  }

  @Override
  public <T> SuccessOrError<T> exec(Callable<T> operation) {
    return new DefaultSuccessOrError<>(operation);
  }

  private class DefaultSuccessOrError<T> implements SuccessOrError<T> {

    private final Callable<T> blockingAction;

    DefaultSuccessOrError(Callable<T> blockingAction) {
      this.blockingAction = blockingAction;
    }

    @Override
    public Success<T> onError(final Action<? super Throwable> errorHandler) {
      return new DefaultSuccess<>(blockingAction, new Action<Throwable>() {
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
      new DefaultSuccess<>(blockingAction, new ForwardToContextErrorHandler()).then(then);
    }

    class ForwardToContextErrorHandler implements Action<Throwable> {
      @Override
      public void execute(Throwable t) {
        if (t instanceof Exception) {
          context.error((Exception) t);
        } else {
          // TODO this is not good enough
          context.error(new ExecutionException(t));
        }
      }
    }
  }

  private class DefaultSuccess<T> implements Success<T> {

    private final Callable<T> blockingAction;
    private final Action<Throwable> errorHandler;

    DefaultSuccess(Callable<T> blockingAction, Action<Throwable> errorHandler) {
      this.blockingAction = blockingAction;
      this.errorHandler = errorHandler;
    }

    @Override
    public void then(final Action<? super T> then) {
      final ListenableFuture<T> future = blockingExecutor.submit(blockingAction);
      Futures.addCallback(future, new FutureCallback<T>() {
        @Override
        public void onSuccess(T result) {
          try {
            then.execute(result);
          } catch (Exception e) {
            context.error(e);
          }
        }

        @Override
        public void onFailure(Throwable t) {
          errorHandler.execute(t);
        }
      }, mainExecutor);
    }
  }
}
