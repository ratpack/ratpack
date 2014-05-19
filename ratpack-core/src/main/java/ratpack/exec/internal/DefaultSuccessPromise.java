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

import ratpack.exec.ExecContext;
import ratpack.exec.ExecController;
import ratpack.exec.Fulfiller;
import ratpack.exec.SuccessPromise;
import ratpack.func.Action;

public class DefaultSuccessPromise<T> implements SuccessPromise<T> {

  private final ExecContext context;
  private final Action<? super Fulfiller<T>> action;
  private final Action<? super Throwable> errorHandler;

  public DefaultSuccessPromise(ExecContext context, Action<? super Fulfiller<T>> action, Action<? super Throwable> errorHandler) {
    this.context = context;
    this.action = action;
    this.errorHandler = errorHandler;
  }

  @Override
  public void then(final Action<? super T> then) {
    final ExecController execController = context.getExecController();
    execController.onExecFinish(new Runnable() {
      @Override
      public void run() {
        try {
          action.execute(new Fulfiller<T>() {
            @Override
            public void error(final Throwable throwable) {
              execController.exec(context.getSupplier(), new Action<ExecContext>() {
                @Override
                public void execute(ExecContext context) throws Exception {
                  errorHandler.execute(throwable);
                }
              });
            }

            @Override
            public void success(final T value) {
              execController.exec(context.getSupplier(), new Action<ExecContext>() {
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
