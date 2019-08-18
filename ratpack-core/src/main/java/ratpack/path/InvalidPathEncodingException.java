/*
 * Copyright 2016 the original author or authors.
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

package ratpack.path;

/**
 * Thrown when a request is made for a path that is not correctly encoded.
 *
 * @since 1.5
 */
public class InvalidPathEncodingException extends RuntimeException {

  /**
   * Constructs the exception.
   *
   * @param cause The underlying exception cause
   */
  public InvalidPathEncodingException(Throwable cause) {
    super(cause);
  }

}
