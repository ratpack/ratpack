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

package ratpack.exec.internal;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import ratpack.exec.ExecContext;
import ratpack.exec.Foreground;
import ratpack.func.Action;
import ratpack.exec.ExecInterceptor;
import ratpack.promise.Fulfiller;
import ratpack.promise.SuccessOrErrorPromise;

import java.util.List;
import java.util.concurrent.Callable;

public class DefaultBackground implements Background {

  private final Foreground foreground;
  private final ListeningExecutorService backgroundExecutor;

  public DefaultBackground(Foreground foreground, ListeningExecutorService backgroundExecutor) {
    this.foreground = foreground;
    this.backgroundExecutor = backgroundExecutor;
  }

  @Override
  public <T> SuccessOrErrorPromise<T> exec(final ExecContext context, final Callable<T> operation, final List<ExecInterceptor> interceptors) {
    return context.promise(new Action<Fulfiller<? super T>>() {
      @Override
      public void execute(final Fulfiller<? super T> fulfiller) throws Exception {
        ListenableFuture<T> future = backgroundExecutor.submit(new BackgroundOperation());
        Futures.addCallback(future, new ForegroundResume(fulfiller), foreground.getExecutor());
      }

      class BackgroundOperation implements Callable<T> {
        private Exception exception;
        private T result;

        @Override
        public T call() throws Exception {
          foreground.exec(context.getSupplier(), interceptors, ExecInterceptor.ExecType.BACKGROUND, new Action<ExecContext>() {
            @Override
            public void execute(ExecContext thing) throws Exception {
              try {
                result = operation.call();
              } catch (Exception e) {
                exception = e;
              }
            }
          });

          if (exception != null) {
            throw exception;
          } else {
            return result;
          }
        }
      }

      class ForegroundResume implements FutureCallback<T> {
        private final Fulfiller<? super T> fulfiller;

        public ForegroundResume(Fulfiller<? super T> fulfiller) {
          this.fulfiller = fulfiller;
        }

        @Override
        public void onSuccess(final T result) {
          fulfiller.success(result);
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public void onFailure(final Throwable t) {
          fulfiller.error(t);
        }
      }
    });
  }

  @Override
  public ListeningExecutorService getExecutor() {
    return backgroundExecutor;
  }
}
