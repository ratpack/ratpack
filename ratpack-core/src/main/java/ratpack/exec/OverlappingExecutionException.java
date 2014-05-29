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
 * An instance of this exception will be logged when execution overlaps.
 */
public class OverlappingExecutionException extends RuntimeException {

  private static final long serialVersionUID = 0;

  /**
   * Constructor.
   *
   * @param message exception message
   */
  public OverlappingExecutionException(String message) {
    super(message);
  }

  /**
   * Constructor.
   *
   * @param message exception message
   * @param cause the exception thrown during overlapping execution
   */
  public OverlappingExecutionException(String message, Throwable cause) {
    super(message, cause);
  }
}
