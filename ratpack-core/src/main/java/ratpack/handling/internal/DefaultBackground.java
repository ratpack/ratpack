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

package ratpack.handling.internal;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import ratpack.func.Action;
import ratpack.handling.Background;
import ratpack.handling.Context;
import ratpack.handling.ProcessingInterceptor;
import ratpack.promise.SuccessOrErrorPromise;
import ratpack.promise.internal.DefaultPromiseFactory;
import ratpack.promise.internal.Fulfiller;
import ratpack.promise.internal.PromiseFactory;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class DefaultBackground implements Background {

  private final ExecutorService foregroundExecutor;
  private final ListeningExecutorService backgroundExecutor;
  private final ContextStorage contextStorage;
  private final PromiseFactory promiseFactory = new DefaultPromiseFactory();

  public DefaultBackground(ExecutorService foregroundExecutor, ListeningExecutorService backgroundExecutor, ContextStorage contextStorage) {
    this.foregroundExecutor = foregroundExecutor;
    this.backgroundExecutor = backgroundExecutor;
    this.contextStorage = contextStorage;
  }

  @Override
  public <T> SuccessOrErrorPromise<T> exec(final Callable<T> operation) {
    final Context context = contextStorage.get();
    final FinishedOnThreadCallbackManager finishedOnThreadCallbackManager = context.get(FinishedOnThreadCallbackManager.class);
    return promiseFactory.promise(contextStorage.get(), new Action<Fulfiller<? super T>>() {
      @Override
      public void execute(final Fulfiller<? super T> fulfiller) throws Exception {
        finishedOnThreadCallbackManager.register(new Runnable() {
          @Override
          public void run() {
            List<ProcessingInterceptor> interceptors = context.getAll(ProcessingInterceptor.class);
            ListenableFuture<T> future = backgroundExecutor.submit(new BackgroundOperation(interceptors));
            Futures.addCallback(future, new ForegroundResume(interceptors, fulfiller), foregroundExecutor);
          }
        });
      }

      class BackgroundOperation extends InterceptedOperation implements Callable<T> {
        private Exception exception;
        private T result;

        public BackgroundOperation(List<ProcessingInterceptor> interceptors) {
          super(ProcessingInterceptor.Type.BACKGROUND, interceptors, context);
        }

        @Override
        public T call() throws Exception {
          contextStorage.set(context);
          try {
            run();
            if (exception != null) {
              throw exception;
            } else {
              return result;
            }
          } finally {
            contextStorage.remove();
          }
        }

        @Override
        protected void performOperation() {
          try {
            result = operation.call();
          } catch (Exception e) {
            exception = e;
          }
        }
      }

      class ForegroundResume implements FutureCallback<T> {

        private final List<ProcessingInterceptor> interceptors;
        private final Fulfiller<? super T> fulfiller;
        private Exception exception;

        public ForegroundResume(List<ProcessingInterceptor> interceptors, Fulfiller<? super T> fulfiller) {
          this.interceptors = interceptors;
          this.fulfiller = fulfiller;
        }

        @Override
        public void onSuccess(final T result) {
          contextStorage.set(context);
          try {
            new InterceptedOperation(ProcessingInterceptor.Type.FOREGROUND, interceptors, context) {
              @Override
              protected void performOperation() {
                try {
                  fulfiller.onSuccess(result);
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
            contextStorage.remove();
          }
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public void onFailure(final Throwable t) {
          contextStorage.set(context);
          try {
            new InterceptedOperation(ProcessingInterceptor.Type.FOREGROUND, interceptors, context) {
              @Override
              protected void performOperation() {
                try {
                  fulfiller.onError(t);
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
            contextStorage.remove();
          }
        }
      }
    });
  }
}
