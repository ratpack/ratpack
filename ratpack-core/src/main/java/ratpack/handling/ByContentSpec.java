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

package ratpack.handling;

import ratpack.func.Block;

/**
 * A specification of how to respond to a request, based on the requested content type (i.e. the request's Accept header).
 * <p>
 * If there is no type registered, or if the client does not accept any of the given types, the "noMatch" handler will be used.
 * By default, the "noMatch" handler will issue a {@code 406} error via {@link Context#clientError(int)}.
 * If you want a different behavior, use {@link #noMatch}.
 * <p>
 * If the request lacks a usable Accept header (header not present or has an empty value), the "unspecified" handler will be used.
 * By default, the "unspecified" handler will use the handler for the first registered content type.
 * If you want a different behavior, use {@link #unspecified}.
 *
 * @see Context#byContent(ratpack.func.Action)
 * @see <a href="http://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231: Accept</a>
 * @see <a href="http://tools.ietf.org/html/rfc7231#section-6.5.6">RFC 7231: 406 Not Acceptable</a>
 */
public interface ByContentSpec {

  /**
   * Specifies that the given handler should be used if the client wants content of the given MIME type.
   * This only supports fully-specified content types (no "*" wildcards).
   *
   * @param mimeType the MIME type to register for
   * @param block the code to invoke if the content type matches
   * @return this
   */
  ByContentSpec type(String mimeType, Block block);

  /**
   * Specifies that the given handler should be used if the client wants content of type "text/plain".
   *
   * @param block the code to invoke if the content type matches
   * @return this
   */
  ByContentSpec plainText(Block block);

  /**
   * Specifies that the given handler should be used if the client wants content of type "text/html".
   *
   * @param block the code to invoke if the content type matches
   * @return this
   */
  ByContentSpec html(Block block);

  /**
   * Specifies that the given handler should be used if the client wants content of type "application/json".
   *
   * @param block the code to invoke if the content type matches
   * @return this
   */
  ByContentSpec json(Block block);

  /**
   * Specifies that the given handler should be used if the client wants content of type "application/xml".
   *
   * @param block the code to invoke if the content type matches
   * @return this
   */
  ByContentSpec xml(Block block);

  /**
   * Specifies that the given handler should be used if the client's requested content type cannot be matched with any of the other handlers.
   *
   * @param block the code to invoke if the content type doesn't match
   * @return this
   */
  ByContentSpec noMatch(Block block);

  /**
   * Specifies that the handler for the specified content type should be used if the client's requested content type cannot be matched with any of the other handlers.
   * Effectively, this treats the request as if the user requested the specified MIME type.
   *
   * @param mimeType the MIME type to use as a fallback if the requested type can't be matched
   * @return this
   */
  ByContentSpec noMatch(String mimeType);

  /**
   * Specifies that the given handler should be used if the client did not provide a usable "Accept" header in the request.
   *
   * @param block the code to invoke if no usable "Accept" header is present in the request.
   * @return this
   * @since 1.5
   */
  ByContentSpec unspecified(Block block);

  /**
   * Specifies that the handler for the specified content type should be used if the client did not provide a usable "Accept" header in the request.
   * Effectively, this treats the request as if the user requested the specified MIME type.
   *
   * @param mimeType the MIME type to use as a fallback if no type is requested
   * @return this
   * @since 1.5
   */
  ByContentSpec unspecified(String mimeType);

}
