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

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;

import java.util.Collections;

/**
 * Thrown when a request is made to parse the request, but no suitable parser was found that matched the content type and parse object.
 *
 * @see ratpack.handling.Context#parse(Parse)
 */
public class NoSuchParserException extends ParseException {

  private static final long serialVersionUID = 0;
  private static final String NEWLINE = System.getProperty("line.separator");

  private final TypeToken<?> type;
  private final Object opts;
  private final String contentType;

  /**
   * Constructor.
   *
   * @param type the target type
   * @param opts the parse options
   * @param contentType The content type of the request
   */
  public NoSuchParserException(TypeToken<?> type, Object opts, String contentType) {
    super(getMessage(type, opts, contentType, Collections.emptyList()));
    this.type = type;
    this.opts = opts;
    this.contentType = contentType;
  }

  /**
   * Constructor.
   *
   * @param type the target type
   * @param opts the parse options
   * @param contentType The content type of the request
   * @param parsers the parsers that had the opportunity to parse but didn't
   */
  public NoSuchParserException(TypeToken<?> type, Object opts, String contentType, Iterable<Parser<?>> parsers) {
    super(getMessage(type, opts, contentType, parsers));
    this.type = type;
    this.opts = opts;
    this.contentType = contentType;
  }

  private static String getMessage(TypeToken<?> type, Object opts, String contentType, Iterable<Parser<?>> parsers) {
    String message = "Could not find parser able to produce object of type '" + type + "' from request of content type '" + contentType + "'" + (opts == null ? "" : " and parse options '" + opts + "'");
    String used = Joiner.on(NEWLINE).join(Iterables.transform(parsers, p -> "  - ".concat(p.toString())));
    if (!used.isEmpty()) {
      message += ". The following parsers were tried:" + NEWLINE + used;
    }
    return message;
  }

  /**
   * The target type.
   *
   * @return the target type
   */
  public TypeToken<?> getType() {
    return type;
  }

  /**
   * The parse opts.
   *
   * @return the parse opts
   */
  public Object getOpts() {
    return opts;
  }

  /**
   * The content type of the request.
   *
   * @return the content type of the request
   */
  public String getContentType() {
    return contentType;
  }

}
