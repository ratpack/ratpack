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

import com.google.common.reflect.TypeToken;

/**
 * Thrown when a request is made to parse the request, but no suitable parser was found that matched the content type and parse object.
 *
 * @see ratpack.handling.Context#parse(Parse)
 */
public class NoSuchParserException extends ParseException {

  private static final long serialVersionUID = 0;

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
    super("Could not find parser able to produce object of type '" + type + "' from request of content type '" + contentType + "'" + (opts == null ? "" : " and parse options '" + opts + "'"));
    this.type = type;
    this.opts = opts;
    this.contentType = contentType;
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
