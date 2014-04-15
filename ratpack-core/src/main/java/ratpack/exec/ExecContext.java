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

package ratpack.exec;

import ratpack.api.NonBlocking;
import ratpack.func.Action;
import ratpack.http.client.HttpClient;
import ratpack.launch.LaunchConfig;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * An execution context.
 */
public interface ExecContext {

  /**
   * Returns this.
   *
   * @return this.
   */
  ExecContext getContext();

  /**
   * Supplies the current active context at any given time.
   * <p>
   * During execution, the context may change.
   * This supplier always returns the current context.
   * <p>
   * This is rarely, if ever, needed in application code.
   * It is used internally by Ratpack to handle contexts jumping across threads.
   */
  interface Supplier {

    /**
     * The current active context.
     *
     * @return the current active context.
     */
    ExecContext get();
  }

  /**
   * A context supplier.
   *
   * @return a context supplier
   * @see ExecContext.Supplier
   */
  Supplier getSupplier();

  @NonBlocking
  void error(Exception exception);

  /**
   * The execution controller.
   *
   * @return the execution controller
   */
  ExecController getExecController();

  List<ExecInterceptor> getInterceptors();

  LaunchConfig getLaunchConfig();

  <T> SuccessOrErrorPromise<T> blocking(Callable<T> blockingOperation);

  <T> SuccessOrErrorPromise<T> promise(Action<? super Fulfiller<T>> action);

  HttpClient getHttpClient();

}
