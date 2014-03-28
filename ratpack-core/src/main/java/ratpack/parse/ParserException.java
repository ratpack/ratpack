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
 * Wraps an exception thrown by a parser while parsing.
 * <p>
 * The cause of this exception is always the exception thrown by the parser.
 *
 * @see ratpack.handling.Context#parse(Parse)
 */
public class ParserException extends ParseException {

  private static final long serialVersionUID = 0;

  /**
   * Constructor.
   *
   * @param parser the parser that failed
   * @param cause the exception thrown by the parser
   */
  public ParserException(Parser<?> parser, Throwable cause) {
    super("Parser '" + parser + "' failed to parse the request", cause);
  }

}
