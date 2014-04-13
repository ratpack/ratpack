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
import ratpack.exec.ExecInterceptor;
import ratpack.exec.Foreground;
import ratpack.func.Action;
import ratpack.http.client.HttpClient;
import ratpack.http.client.HttpClients;
import ratpack.promise.Fulfiller;
import ratpack.promise.SuccessOrErrorPromise;
import ratpack.promise.internal.DefaultSuccessOrErrorPromise;

import java.util.List;
import java.util.concurrent.Callable;

public abstract class AbstractExecContext implements ExecContext {

  private final Foreground foreground;
  private final Background background;

  protected AbstractExecContext(Foreground foreground, Background background) {
    this.foreground = foreground;
    this.background = background;
  }

  @Override
  public ExecContext getContext() {
    return this;
  }

  @Override
  public Foreground getForeground() {
    return foreground;
  }

  @Override
  public <T> SuccessOrErrorPromise<T> background(Callable<T> backgroundOperation) {
    return background.exec(this, backgroundOperation, getExecInterceptors());
  }

  abstract protected List<ExecInterceptor> getExecInterceptors();

  @Override
  public <T> SuccessOrErrorPromise<T> promise(Action<? super Fulfiller<T>> action) {
    return new DefaultSuccessOrErrorPromise<>(this, getForeground(), getExecInterceptors(), action);
  }

  @Override
  public HttpClient getHttpClient() {
    return HttpClients.httpClient(this);
  }
}
