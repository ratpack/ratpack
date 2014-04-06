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
import ratpack.promise.SuccessOrErrorPromise;
import ratpack.promise.SuccessPromise;

import static ratpack.util.ExceptionUtils.toException;

public class DefaultSuccessOrErrorPromise<T> implements SuccessOrErrorPromise<T> {
  private final Context context;
  private final Action<? super Fulfiller<? super T>> action;

  public DefaultSuccessOrErrorPromise(Context context, Action<? super Fulfiller<? super T>> action) {
    this.context = context;
    this.action = action;
  }

  @Override
  public SuccessPromise<T> onError(final Action<? super Throwable> errorHandler) {
    return new DefaultSuccessPromise<>(action, new ForwardToContextErrorHandler(), new Action<Throwable>() {
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
  public void then(Action<? super T> then) throws Exception {
    new DefaultSuccessPromise<>(action, new ForwardToContextErrorHandler()).then(then);
  }

  private class ForwardToContextErrorHandler implements Action<Throwable> {
    @Override
    public void execute(Throwable t) {
      context.error(toException(t));
    }
  }
}
