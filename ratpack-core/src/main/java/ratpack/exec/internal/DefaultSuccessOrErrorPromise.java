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

import ratpack.exec.*;
import ratpack.func.Action;

import static ratpack.util.ExceptionUtils.toException;

public class DefaultSuccessOrErrorPromise<T> implements SuccessOrErrorPromise<T> {
  private final ExecContext context;
  private final ExecController execController;
  private final Action<? super Fulfiller<T>> action;

  public DefaultSuccessOrErrorPromise(ExecContext context, ExecController execController, Action<? super Fulfiller<T>> action) {
    this.context = context;
    this.execController = execController;

    this.action = action;
  }

  @Override
  public SuccessPromise<T> onError(final Action<? super Throwable> errorHandler) {
    return new DefaultSuccessPromise<>(context, execController, action, errorHandler);
  }

  @Override
  public void then(Action<? super T> then) {
    onError(new Action<Throwable>() {
      @Override
      public void execute(Throwable t) throws Exception {
        context.error(toException(t));
      }
    }).then(then);
  }

}
