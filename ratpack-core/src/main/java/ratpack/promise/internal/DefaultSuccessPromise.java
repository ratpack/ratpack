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
import ratpack.handling.Context;
import ratpack.handling.internal.ContextStorage;
import ratpack.promise.Fulfiller;
import ratpack.promise.SuccessPromise;

public class DefaultSuccessPromise<T> implements SuccessPromise<T> {

  private final ContextStorage contextStorage;
  private final Action<? super Fulfiller<T>> action;
  private final Action<? super Throwable> operationErrorHandler;
  private final Action<? super Throwable> globalErrorHandler;

  public DefaultSuccessPromise(ContextStorage contextStorage, Action<? super Fulfiller<T>> action, Action<? super Throwable> globalErrorHandler, Action<? super Throwable> operationErrorHandler) {
    this.contextStorage = contextStorage;
    this.action = action;
    this.operationErrorHandler = operationErrorHandler;
    this.globalErrorHandler = globalErrorHandler;
  }

  public DefaultSuccessPromise(ContextStorage contextStorage, Action<? super Fulfiller<T>> action, Action<? super Throwable> errorHandler) {
    this(contextStorage, action, errorHandler, errorHandler);
  }

  @Override
  public void then(final Action<? super T> then) throws Exception {
    final Context context = contextStorage.get();

    action.execute(new Fulfiller<T>() {
      @Override
      public void error(Throwable throwable) {
        boolean contextStorageBound = contextStorage.get() != null;
        try {
          if (!contextStorageBound) {
            contextStorage.set(context);
          }
          operationErrorHandler.execute(throwable);
        } catch (Throwable e) {
          if (operationErrorHandler == globalErrorHandler) {
            e.printStackTrace();
          } else {
            try {
              globalErrorHandler.execute(throwable);
            } catch (Exception e1) {
              e1.printStackTrace();
            }
          }
        } finally {
          if (!contextStorageBound) {
            contextStorage.remove();
          }
        }
      }

      @Override
      public void success(T value) {
        boolean contextStorageBound = contextStorage.get() != null;
        if (!contextStorageBound) {
          contextStorage.set(context);
        }
        try {
          then.execute(value);
        } catch (Throwable e) {
          try {
            globalErrorHandler.execute(e);
          } catch (Throwable e1) {
            e1.printStackTrace();
          }
        } finally {
          if (!contextStorageBound) {
            contextStorage.remove();
          }
        }
      }
    });
  }

}
