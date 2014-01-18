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

package ratpack.parse;

/**
 * A generic super type for exceptions indicate something when wrong for a parse operation.
 *
 * @see ParserException
 * @see NoSuchParserException
 */
public abstract class ParseException extends RuntimeException {

  private static final long serialVersionUID = 0;

  /**
   * Constructor.
   *
   * @param message the exception message.
   */
  protected ParseException(String message) {
    super(message);
  }

  /**
   * Constructor.
   *
   * @param message the exception message.
   * @param cause the exception to wrap
   */
  protected ParseException(String message, Throwable cause) {
    super(message, cause);
  }

}
