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
 * The context of an execution.
 * <p>
 * An “execution” is a <i>logical</i> unit of work (e.g. handling a request, performing a background job).
 * As execution in Ratpack is asynchronous, the execution may actually span multiple threads.
 * This type provides control primitives (i.e. {@link #blocking(Callable)}, {@link #promise(Action)}) that facilitate a logical execution executing asynchronously.
 */
public interface ExecContext extends ExecControl {

  /**
   * Returns {@code this}.
   *
   * @return {@code this}.
   */
  ExecContext getContext();

  /**
   * Supplies the current effective context of the execution at any given time.
   * <p>
   * An exec context may provide ways to change the exec context for part of an execution.
   * This supplier always returns the effective context, which might not be {@code this}.
   * <p>
   * This is rarely, if ever, needed in application code.
   * It is used internally by Ratpack to handle contexts jumping across threads.
   */
  interface Supplier {

    /**
     * The current effective execution context.
     *
     * @return the current effective execution context
     */
    ExecContext get();
  }

  /**
   * The supplier of the effective context for the current execution.
   *
   * @return the supplier of the effective context for the current execution
   * @see ExecContext.Supplier
   */
  Supplier getSupplier();

  /**
   * Terminate this execution with the given exception.
   * <p>
   * Generally, it is preferable to just throw an exception rather than using this method.
   * Such an exception will be caught by the infrastructure, then forwarded to this method.
   * <p>
   * This method MUST NOT throw exceptions.
   * Care should be taken to ensure that any exceptions are dealt with.
   *
   * @param exception the exception that should terminate the execution
   */
  @NonBlocking
  void error(Exception exception);

  /**
   * The execution controller.
   *
   * @return the execution controller
   */
  ExecController getExecController();

  /**
   * The execution interceptors.
   *
   * @return the execution interceptors
   */
  List<ExecInterceptor> getInterceptors();

  /**
   * The application launch config.
   *
   * @return the application launch config
   */
  LaunchConfig getLaunchConfig();

  /**
   * {@inheritDoc}
   */
  @Override
  <T> Promise<T> blocking(Callable<T> blockingOperation);

  /**
   * {@inheritDoc}
   */
  @Override
  <T> Promise<T> promise(Action<? super Fulfiller<T>> action);

  HttpClient getHttpClient();

}
