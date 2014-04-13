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
import ratpack.promise.SuccessOrErrorPromise;
import ratpack.promise.SuccessPromise;

import java.util.List;

import static ratpack.util.ExceptionUtils.toException;

public class DefaultSuccessOrErrorPromise<T> implements SuccessOrErrorPromise<T> {
  private final ExecContext context;
  private final Foreground foreground;
  private final List<ExecInterceptor> interceptors;
  private final Action<? super Fulfiller<T>> action;

  public DefaultSuccessOrErrorPromise(ExecContext context, Foreground foreground, List<ExecInterceptor> interceptors, Action<? super Fulfiller<T>> action) {
    this.context = context;
    this.foreground = foreground;
    this.interceptors = interceptors;

    this.action = action;
  }

  @Override
  public SuccessPromise<T> onError(final Action<? super Throwable> errorHandler) {
    return new DefaultSuccessPromise<>(context, foreground, interceptors, action, errorHandler);
  }

  @Override
  public void then(Action<? super T> then) {
    onError(new Action<Throwable>() {
      @Override
      public void execute(Throwable t) {
        context.error(toException(t));
      }
    }).then(then);
  }

}
