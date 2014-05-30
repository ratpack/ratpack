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

/**
 * Can be thrown to signal that the current execution segment has fatally errored.
 * <p>
 * This can be thrown to unravel the call stack without being caught be exception handlers.
 * The cause of this exception will be forwarded to the execution error handler.
 * <p>
 * This exception should not be caught or thrown by user code.
 * It is used when integrating with an asynchronous execution framework.
 */
public class ExecutionSegmentTerminationError extends Error {

  private static final long serialVersionUID = 0;

  /**
   * Constructor.
   *
   * @param cause the real exception
   */
  public ExecutionSegmentTerminationError(Throwable cause) {
    super(cause);
  }
}
