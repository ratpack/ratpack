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
 * This is useful when a given handler can provide content of more than one type (i.e. <a href="http://en.wikipedia.org/wiki/Content_negotiation">content negotiation</a>).
 * <p>
 * The handler to use will be selected based on parsing the "Accept" header, respecting quality weighting and wildcard matching.
 * The order that types are specified is significant for wildcard matching.
 * The earliest registered type that matches the wildcard will be used.
 *
 * <pre class="java">{@code
 * import ratpack.test.embed.EmbeddedApp;
 * import ratpack.http.client.ReceivedResponse;
 *
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *   public static void main(String[] args) throws Exception {
 *     EmbeddedApp.fromHandler(ctx -> {
 *       String message = "hello!";
 *       ctx.byContent(m -> m
 *         .json(() -> ctx.render("{\"msg\": \"" + message + "\"}"))
 *         .html(() -> ctx.render("<p>" + message + "</p>"))
 *       );
 *     }).test(httpClient -> {
 *       ReceivedResponse response = httpClient.requestSpec(s -> s.getHeaders().add("Accept", "application/json")).get();
 *       assertEquals("{\"msg\": \"hello!\"}", response.getBody().getText());
 *       assertEquals("application/json", response.getBody().getContentType().getType());
 *
 *       response = httpClient.requestSpec(s -> s.getHeaders().add("Accept", "text/plain; q=1.0, text/html; q=0.8, application/json; q=0.7")).get();
 *       assertEquals("<p>hello!</p>", response.getBody().getText());
 *       assertEquals("text/html", response.getBody().getContentType().getType());
 *     });
 *   }
 * }
 * }</pre>
 * <p>
 * If there is no type registered, or if the client does not accept any of the given types, the "noMatch" handler will be used.
 * By default, the "noMatch" handler will issue a {@code 406} error via {@link Context#clientError(int)}.
 * If you want a different behavior, use {@link #noMatch}.
 * <p>
 * If the request lacks a usable Accept header (header not present or has an empty value), the "unspecified" handler will be used.
 * By default, the "unspecified" handler will use the handler for the first registered content type.
 * If you want a different behavior, use {@link #unspecified}.
 * <p>
 * Only the last specified handler for a type will be used.
 * That is, adding a subsequent handler for the same type will replace the previous.
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
  default ByContentSpec type(String mimeType, Block block) {
    return type(mimeType, Handlers.of(block));
  }

  /**
   * Specifies that the given handler should be used if the client wants content of the given MIME type.
   * This only supports fully-specified content types (no "*" wildcards).
   *
   * @param mimeType the MIME type to register for
   * @param block the code to invoke if the content type matches
   * @return this
   * @since 1.6
   */
  default ByContentSpec type(CharSequence mimeType, Block block) {
    return type(mimeType, Handlers.of(block));
  }

  /**
   * Specifies that the given handler should be used if the client wants content of the given MIME type.
   * This only supports fully-specified content types (no "*" wildcards).
   *
   * @param mimeType the MIME type to register for
   * @param handler the handler to invoke if the content type matches
   * @return this
   * @since 1.5
   */
  ByContentSpec type(String mimeType, Handler handler);

  /**
   * Specifies that the given handler should be used if the client wants content of the given MIME type.
   * This only supports fully-specified content types (no "*" wildcards).
   *
   * @param mimeType the MIME type to register for
   * @param handler the handler to invoke if the content type matches
   * @return this
   * @since 1.6
   */
  ByContentSpec type(CharSequence mimeType, Handler handler);

  /**
   * Specifies that the given handler should be used if the client wants content of the given MIME type.
   * This only supports fully-specified content types (no "*" wildcards).
   *
   * @param mimeType the MIME type to register for
   * @param handlerType the type of handler to retrieve from the registry and use
   * @return this
   * @since 1.5
   */
  ByContentSpec type(String mimeType, Class<? extends Handler> handlerType);

  /**
   * Specifies that the given handler should be used if the client wants content of the given MIME type.
   * This only supports fully-specified content types (no "*" wildcards).
   *
   * @param mimeType the MIME type to register for
   * @param handlerType the type of handler to retrieve from the registry and use
   * @return this
   * @since 1.6
   */
  ByContentSpec type(CharSequence mimeType, Class<? extends Handler> handlerType);

  /**
   * Specifies that the given handler should be used if the client wants content of type "text/plain".
   *
   * @param block the code to invoke if the content type matches
   * @return this
   */
  default ByContentSpec plainText(Block block) {
    return plainText(Handlers.of(block));
  }

  /**
   * Specifies that the given handler should be used if the client wants content of type "text/plain".
   *
   * @param handler the handler to invoke if the content type matches
   * @return this
   * @since 1.5
   */
  ByContentSpec plainText(Handler handler);

  /**
   * Specifies that the given handler should be used if the client wants content of type "text/plain".
   *
   * @param handlerType the type of handler to retrieve from the registry and use
   * @return this
   * @since 1.5
   */
  ByContentSpec plainText(Class<? extends Handler> handlerType);

  /**
   * Specifies that the given handler should be used if the client wants content of type "text/html".
   *
   * @param block the code to invoke if the content type matches
   * @return this
   */
  default ByContentSpec html(Block block) {
    return html(Handlers.of(block));
  }

  /**
   * Specifies that the given handler should be used if the client wants content of type "text/html".
   *
   * @param handler the handler to invoke if the content type matches
   * @return this
   * @since 1.5
   */
  ByContentSpec html(Handler handler);

  /**
   * Specifies that the given handler should be used if the client wants content of type "text/html".
   *
   * @param handlerType the type of handler to retrieve from the registry and use
   * @return this
   * @since 1.5
   */
  ByContentSpec html(Class<? extends Handler> handlerType);

  /**
   * Specifies that the given handler should be used if the client wants content of type "application/json".
   *
   * @param block the code to invoke if the content type matches
   * @return this
   */
  default ByContentSpec json(Block block) {
    return json(Handlers.of(block));
  }

  /**
   * Specifies that the given handler should be used if the client wants content of type "application/json".
   *
   * @param handler the handler to invoke if the content type matches
   * @return this
   * @since 1.5
   */
  ByContentSpec json(Handler handler);

  /**
   * Specifies that the given handler should be used if the client wants content of type "application/json".
   *
   * @param handlerType the type of handler to retrieve from the registry and use
   * @return this
   * @since 1.5
   */
  ByContentSpec json(Class<? extends Handler> handlerType);

  /**
   * Specifies that the given handler should be used if the client wants content of type "application/xml".
   *
   * @param block the code to invoke if the content type matches
   * @return this
   */
  default ByContentSpec xml(Block block) {
    return xml(Handlers.of(block));
  }

  /**
   * Specifies that the given handler should be used if the client wants content of type "application/xml".
   *
   * @param handler the handler to invoke if the content type matches
   * @return this
   * @since 1.5
   */
  ByContentSpec xml(Handler handler);

  /**
   * Specifies that the given handler should be used if the client wants content of type "application/xml".
   *
   * @param handlerType the type of handler to retrieve from the registry and use
   * @return this
   * @since 1.5
   */
  ByContentSpec xml(Class<? extends Handler> handlerType);

  /**
   * Specifies that the given handler should be used if the client's requested content type cannot be matched with any of the other handlers.
   *
   * @param block the code to invoke if the content type doesn't match
   * @return this
   */
  default ByContentSpec noMatch(Block block) {
    return noMatch(Handlers.of(block));
  }

  /**
   * Specifies that the given handler should be used if the client's requested content type cannot be matched with any of the other handlers.
   *
   * @param handler the handler to invoke if the content type matches
   * @return this
   * @since 1.5
   */
  ByContentSpec noMatch(Handler handler);

  /**
   * Specifies that the given handler should be used if the client's requested content type cannot be matched with any of the other handlers.
   *
   * @param handlerType the type of handler to retrieve from the registry and use
   * @return this
   * @since 1.5
   */
  ByContentSpec noMatch(Class<? extends Handler> handlerType);

  /**
   * Specifies that the handler for the specified content type should be used if the client's requested content type cannot be matched with any of the other handlers.
   * Effectively, this treats the request as if the user requested the specified MIME type.
   * If the specified mimeType doesn't have a registered block, it will result in a server error for applicable requests.
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
  default ByContentSpec unspecified(Block block) {
    return unspecified(Handlers.of(block));
  }

  /**
   * Specifies that the given handler should be used if the client did not provide a usable "Accept" header in the request.
   *
   * @param handler the handler to invoke if if no usable "Accept" header is present in the request.
   * @return this
   * @since 1.5
   */
  ByContentSpec unspecified(Handler handler);

  /**
   * Specifies that the given handler should be used if the client did not provide a usable "Accept" header in the request.
   *
   * @param handlerType the type of handler to retrieve from the registry and use if no usable "Accept" header is present in the request.
   * @return this
   * @since 1.5
   */
  ByContentSpec unspecified(Class<? extends Handler> handlerType);

  /**
   * Specifies that the handler for the specified content type should be used if the client did not provide a usable "Accept" header in the request.
   * Effectively, this treats the request as if the user requested the specified MIME type.
   * If the specified mimeType doesn't have a registered block, it will result in a server error for applicable requests.
   *
   * @param mimeType the MIME type to use as a fallback if no type is requested
   * @return this
   * @since 1.5
   */
  ByContentSpec unspecified(String mimeType);

}
