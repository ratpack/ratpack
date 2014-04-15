/*
 * Copyright 2013 the original author or authors.
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

/**
 * Wraps an exception thrown during an execution.
 * <p>
 * When an exception is triggered during an execution, this exception should be thrown.
 * It should be constructed with the effective {@link ExecContext} at the time of the exception.
 * <p>
 * All executions are started via {@link ExecController#exec(ExecContext.Supplier, ratpack.func.Action)}.
 * This exception is caught inside this method, then forward to its {@link #getContext() context's} {@link ExecContext#error(Exception)} method.
 * <p>
 * Generally this can all be ignored by user code, particularly request handling.
 * The infrastructure generally manages this mechanism.
 */
public class ExecException extends RuntimeException {

  private static final long serialVersionUID = 0;

  private final ExecContext context;

  /**
   * Constructor.
   *
   * @param context the execution context at the time the exception occurred
   * @param exception the real exception
   */
  public ExecException(ExecContext context, Throwable exception) {
    super(exception);
    this.context = context;
  }

  /**
   * The execution context at the time that the exception occurred.
   *
   * @return the execution context at the time that the exception occurred
   */
  public ExecContext getContext() {
    return context;
  }

  /**
   * Converts, if necessary, the given {@code throwable} into an {@code ExecException}.
   * <p>
   * If the {@code throwable} is an {@code ExecException}, it is returned as is.
   * <p>
   * This method should be used when catching/thrown exceptions by infrastructure.
   * The return value should be immediately thrown.
   *
   * @param context the execution context at the
   * @param throwable the exception to possibly wrap
   * @return an exec exception
   */
  public static ExecException wrap(ExecContext context, Throwable throwable) {
    if (throwable instanceof ExecException) {
      return (ExecException) throwable;
    } else {
      return new ExecException(context, throwable);
    }
  }

}
