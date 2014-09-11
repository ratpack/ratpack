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

package ratpack.test.exec;

import ratpack.exec.ExecControl;
import ratpack.exec.Execution;
import ratpack.exec.Promise;
import ratpack.func.Function;

/**
 * A utility for testing asynchronous support/service code.
 * <p>
 * An execution harness is backed by a thread pool.
 * It is important to call {@link #close()} when the object is no longer needed to shutdown this thread pool.
 * <p>
 * For usage examples, see {@link ratpack.test.UnitTest#execHarness()}
 *
 * @see ratpack.test.UnitTest#execHarness()
 */
public interface ExecHarness extends AutoCloseable {

  /**
   * Synchronously returns a promised value.
   * <p>
   * The given function will execute in a separate thread.
   * The calling thread will block, waiting for the promised value to be provided.
   *
   * @param func a function that exercises some code that returns a promise
   * @param <T> the type of promised value
   * @return the result of the execution
   * @throws Exception any thrown by the function, or the promise failure exception
   */
  public <T> ExecResult<T> execute(Function<Execution, Promise<T>> func) throws Exception;

  /**
   * The execution control for the harness.
   * <p>
   * This is typically given to the code under test to perform the async ops.
   *
   * @return an execution control.
   */
  public ExecControl getControl();

  /**
   * Shuts down the thread pool backing this harness.
   */
  @Override
  void close();

}
