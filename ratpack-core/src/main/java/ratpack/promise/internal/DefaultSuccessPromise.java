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

import ratpack.func.Action;
import ratpack.promise.SuccessPromise;

public class DefaultSuccessPromise<T> implements SuccessPromise<T> {

  private final Action<? super Fulfiller<? super T>> action;
  private final Action<? super Throwable> operationErrorHandler;
  private final Action<? super Throwable> globalErrorHandler;

  public DefaultSuccessPromise(Action<? super Fulfiller<? super T>> action, Action<? super Throwable> globalErrorHandler, Action<? super Throwable> operationErrorHandler) {
    this.action = action;
    this.operationErrorHandler = operationErrorHandler;
    this.globalErrorHandler = globalErrorHandler;
  }

  public DefaultSuccessPromise(Action<? super Fulfiller<? super T>> action, Action<? super Throwable> errorHandler) {
    this(action, errorHandler, errorHandler);
  }

  @Override
  public void then(final Action<? super T> then) throws Exception {
    action.execute(new Fulfiller<T>() {
      @Override
      public void onError(Throwable t) {
        try {
          operationErrorHandler.execute(t);
        } catch (Throwable e) {
          if (operationErrorHandler == globalErrorHandler) {
            e.printStackTrace();
          } else {
            try {
              globalErrorHandler.execute(t);
            } catch (Exception e1) {
              e1.printStackTrace();
            }
          }
        }
      }

      @Override
      public void onSuccess(T thing) {
        try {
          then.execute(thing);
        } catch (Throwable e) {
          try {
            globalErrorHandler.execute(e);
          } catch (Throwable e1) {
            e1.printStackTrace();
          }
        }
      }
    });
  }

}
