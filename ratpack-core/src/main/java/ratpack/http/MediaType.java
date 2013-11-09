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

package ratpack.http;

import ratpack.api.Nullable;

import java.util.Map;

/**
 * A structured value for a Content-Type header value.
 * <p>
 * Can also represent a non existent (i.e. empty) value.
 */
@SuppressWarnings("UnusedDeclaration")
public interface MediaType {

  /**
   * {@value}.
   */
  String APPLICATION_JSON = "application/json";

  /**
   * {@value}.
   */
  String APPLICATION_FORM = "application/x-www-form-urlencoded";

  /**
   * The type without parameters.
   * <p>
   * Given a mime type of "application/json;charset=utf-8", returns "application/json".
   * <p>
   * May be null to represent no content type.
   *
   * @return The mime type without parameters, or null if this represents the absence of a value.
   */
  @Nullable
  String getType();

  /**
   * The parameters of the mime type.
   * <p>
   * Given a mime type of "application/json;charset=utf-8", returns {@code [charset: "utf-8"]}".
   * May be empty, never null.
   * <p>
   * All param names have been lower cased.
   *
   * @return the media type params.
   */
  Map<String, String> getParams();

  /**
   * The value of the "charset" parameter, or the HTTP default of {@code "ISO-8859-1"}.
   * <p>
   * This method always returns a value, even if the actual type is a binary type.
   *
   * @return The value of the charset parameter, or the HTTP default of {@code "ISO-8859-1"}.
   */
  String getCharset();

  /**
   * True if this type starts with "{@code text/}".
   *
   * @return True if this type starts with "{@code text/}".
   */
  boolean isText();

  /**
   * True if this type equals {@value #APPLICATION_JSON}.
   *
   * @return True if this type equals {@value #APPLICATION_JSON}.
   */
  boolean isJson();

  /**
   * True if this type equals {@value #APPLICATION_FORM}.
   *
   * @return True if this type equals {@value #APPLICATION_FORM}.
   */
  boolean isForm();

  /**
   * True if this represents the absence of a value (i.e. no Content-Type header)
   *
   * @return True if this represents the absence of a value (i.e. no Content-Type header)
   */
  boolean isEmpty();
}
