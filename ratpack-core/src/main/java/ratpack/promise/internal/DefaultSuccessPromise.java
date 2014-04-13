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

package ratpack.promise.internal;

import ratpack.exec.ExecContext;
import ratpack.exec.Foreground;
import ratpack.func.Action;
import ratpack.exec.ExecInterceptor;
import ratpack.promise.Fulfiller;
import ratpack.promise.SuccessPromise;

import java.util.List;

public class DefaultSuccessPromise<T> implements SuccessPromise<T> {

  private final ExecContext context;
  private final Foreground foreground;
  private final List<ExecInterceptor> interceptors;
  private final Action<? super Fulfiller<T>> action;
  private final Action<? super Throwable> operationErrorHandler;

  public DefaultSuccessPromise(ExecContext context, Foreground foreground, List<ExecInterceptor> interceptors, Action<? super Fulfiller<T>> action, Action<? super Throwable> operationErrorHandler) {
    this.context = context;
    this.foreground = foreground;
    this.interceptors = interceptors;
    this.action = action;
    this.operationErrorHandler = operationErrorHandler;
  }

  @Override
  public void then(final Action<? super T> then) {
    foreground.onExecFinish(new Runnable() {
      @Override
      public void run() {
        try {
          action.execute(new Fulfiller<T>() {
            @Override
            public void error(final Throwable throwable) {
              foreground.exec(context.getSupplier(), interceptors, ExecInterceptor.ExecType.FOREGROUND, new Action<ExecContext>() {
                @Override
                public void execute(ExecContext context) throws Exception {
                  operationErrorHandler.execute(throwable);
                }
              });
            }

            @Override
            public void success(final T value) {
              foreground.exec(context.getSupplier(), interceptors, ExecInterceptor.ExecType.FOREGROUND, new Action<ExecContext>() {
                @Override
                public void execute(ExecContext context) throws Exception {
                  then.execute(value);
                }
              });
            }
          });
        } catch (Exception e) {
          context.error(e);
        }
      }
    });
  }

}
