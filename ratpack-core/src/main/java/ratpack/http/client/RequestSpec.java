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

package ratpack.http.client;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import ratpack.func.Action;
import ratpack.func.Factory;
import ratpack.func.Function;
import ratpack.http.HttpMethod;
import ratpack.http.MutableHeaders;

import javax.net.ssl.SSLContext;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static java.time.temporal.ChronoUnit.SECONDS;

public interface RequestSpec {

  /**
   * The default number of redirects to follow automatically.
   *
   * @see #redirects(int)
   */
  int DEFAULT_MAX_REDIRECTS = 10;

  /**
   * The maximum number of redirects to automatically follow, before simply returning the redirect response.
   * <p>
   * The default value is {@value #DEFAULT_MAX_REDIRECTS}.
   * <p>
   * The given value must be &gt;= 0.
   *
   * @param maxRedirects the maximum number of redirects to follow
   * @return {@code this}
   */
  RequestSpec redirects(int maxRedirects);

  /**
   * Get the configured maximum number of redirects.
   *
   * @return The maximum number of redirects
   * @see #redirects(int)
   * @since 1.6
   */
  int getRedirects();

  /**
   * Specifies a function to invoke when a redirectable response is received.
   * <p>
   * If the function returns null, the redirect will not be followed.
   * If it returns an action, it will be followed and the action will be invoked to further configure the request spec.
   * To simply follow the redirect, return {@link Action#noop()}.
   * <p>
   * The function will never be invoked if {@link #redirects(int)} is set to 0.
   *
   * @param function the redirection handling strategy
   * @return {@code this}
   */
  RequestSpec onRedirect(Function<? super ReceivedResponse, Action<? super RequestSpec>> function);

  /**
   * Sets the {@link SSLContext} used for client and server SSL authentication.
   *
   * @param sslContext SSL context with keystore as well as trust store
   * @return {@code this}
   * @deprecated since 1.5, use {@link #sslContext(SslContext)}
   */
  @Deprecated
  RequestSpec sslContext(SSLContext sslContext);

  /**
   * Get the configured {@link SslContext} used for client and server SSL authentication.
   *
   * @return The {@code SslContext} used for SSL authentication
   * @see #sslContext(SslContext)
   * @since 1.6
   */
  SslContext getSslContext();

  /**
   * Factory method to create {@link SSLContext} used for client and server SSL authentication.
   *
   * @param factory provides a factory that will create {@link SSLContext} instance
   * @return {@code this}
   * @throws Exception this can be thrown from the action
   * @deprecated since 1.5, no replacement.
   */
  @SuppressWarnings("deprecation")
  @Deprecated
  default RequestSpec sslContext(Factory<SSLContext> factory) throws Exception {
    return sslContext(factory.create());
  }

  /**
   * Sets the {@link SslContext} used for client and server SSL authentication.
   *
   * @param sslContext SSL context with keystore as well as trust store
   * @return the {@link RequestSpec}
   * @see SslContextBuilder#forClient()
   */
  RequestSpec sslContext(SslContext sslContext);

  /**
   * @return {@link ratpack.http.MutableHeaders} that can be used to configure the headers that will be used for the request.
   */
  MutableHeaders getHeaders();

  /**
   * The maximum response length to accept.
   *
   * @param numBytes the maximum response length to accept
   * @return {@code this}
   * @since 1.4
   */
  RequestSpec maxContentLength(int numBytes);

  /**
   * Get the configured maximum response length.
   *
   * @return The maximum response length
   * @see #maxContentLength(int)
   * @since 1.6
   */
  int getMaxContentLength();

  /**
   * The max size of the chunks to emit when reading a response as a stream.
   * <p>
   * Defaults to the configured {@link HttpClientSpec#responseMaxChunkSize(int)} for the corresponding client.
   * <p>
   * Increasing this value can increase throughput at the expense of memory use.
   *
   * @param numBytes the max number of bytes to emit
   * @return {@code this}
   * @since 1.5
   */
  RequestSpec responseMaxChunkSize(int numBytes);

  /**
   * This method can be used to buffer changes to the headers.
   *
   * @param action Provide an action that will act on MutableHeaders.
   * @return {@code this}
   * @throws Exception This can be thrown from the action supplied.
   */
  RequestSpec headers(Action<? super MutableHeaders> action) throws Exception;

  /**
   * Specifies the request method.
   *
   * @param method the method
   * @return {@code this}
   */
  default RequestSpec method(String method) {
    return method(HttpMethod.of(method));
  }

  /**
   * Specifies the request method.
   *
   * @param method the method
   * @return this
   * @since 1.4
   */
  RequestSpec method(HttpMethod method);

  /**
   * Get the configured request method.
   *
   * @return The request method.
   * @see #method(HttpMethod)
   * @since 1.6
   */
  HttpMethod getMethod();

  /**
   * Specifies to use the GET request method.
   *
   * @return {@code this}
   * @since 1.4
   */
  default RequestSpec get() {
    return method(HttpMethod.GET);
  }

  /**
   * Specifies to use the POST request method.
   *
   * @return {@code this}
   * @since 1.4
   */
  default RequestSpec post() {
    return method(HttpMethod.POST);
  }

  /**
   * Specifies to use the PUT request method.
   *
   * @return {@code this}
   * @since 1.4
   */
  default RequestSpec put() {
    return method(HttpMethod.PUT);
  }

  /**
   * Specifies to use the DELETE request method.
   *
   * @return {@code this}
   * @since 1.4
   */
  default RequestSpec delete() {
    return method(HttpMethod.DELETE);
  }

  /**
   * Specifies to use the PATCH request method.
   *
   * @return {@code this}
   * @since 1.4
   */
  default RequestSpec patch() {
    return method(HttpMethod.PATCH);
  }

  /**
   * Specifies to use the OPTIONS request method.
   *
   * @return {@code this}
   * @since 1.4
   */
  default RequestSpec options() {
    return method(HttpMethod.OPTIONS);
  }

  /**
   * Specifies to use the HEAD request method.
   *
   * @return {@code this}
   * @since 1.4
   */
  default RequestSpec head() {
    return method(HttpMethod.HEAD);
  }

  /**
   * Enables automatic decompression of the response.
   *
   * @param shouldDecompress whether to enable decompression
   * @return {@code this}
   */
  RequestSpec decompressResponse(boolean shouldDecompress);

  /**
   * Gets if responses are automatically decompressed.
   *
   * @return Whether response decompression is enabled
   * @see #decompressResponse(boolean)
   * @since 1.6
   */
  boolean getDecompressResponse();

  /**
   * The request URI.
   *
   * @return the request URI
   * @since 1.4
   */
  URI getUri();

  /**
   * @deprecated since 1.4, use {@link #getUri()}
   */
  @Deprecated
  default URI getUrl() {
    return getUri();
  }

  /**
   * @since 1.1
   * @deprecated since 1.4, use {@link #connectTimeout(Duration)}
   */
  @Deprecated
  default RequestSpec connectTimeoutSeconds(int seconds) {
    return connectTimeout(Duration.of(seconds, SECONDS));
  }

  /**
   * Sets the socket connection timeout.
   * <p>
   * This value defaults to 30 seconds.
   *
   * @since 1.1
   * @param duration the socket connection timeout
   * @return {@code this}
   */
  RequestSpec connectTimeout(Duration duration);

  /**
   * Gets the configured socket connection timeout.
   *
   * @return The socket connection timeout
   * @see #connectTimeout(Duration)
   * @since 1.6
   */
  Duration getConnectTimeout();

  /**
   * @deprecated since 1.4, use {@link #readTimeout(Duration)}
   */
  @Deprecated
  default RequestSpec readTimeoutSeconds(int seconds) {
    return readTimeout(Duration.of(seconds, SECONDS));
  }

  /**
   * Sets the socket read timeout.
   * <p>
   * This value defaults to 30 seconds.
   *
   * @param duration the socket read timeout
   * @return {@code this}
   */
  RequestSpec readTimeout(Duration duration);

  /**
   * Gets the configured socket read timeout.
   *
   * @return The socket read timeout
   * @see #readTimeout(Duration)
   * @since 1.6
   */
  Duration getReadTimeout();

  /**
   * The body of the request, used for specifying the body content.
   *
   * @return the (writable) body of the request
   */
  Body getBody();

  /**
   * Executes the given action with the {@link #getBody() request body}.
   * <p>
   * This method is a “fluent API” alternative to {@link #getBody()}.
   *
   * @param action configuration of the request body
   * @return this
   * @throws Exception any thrown by {@code action}
   */
  RequestSpec body(Action<? super Body> action) throws Exception;

  /**
   * Adds the appropriate header for HTTP Basic authentication with the given username and password.
   * <p>
   * This will replace any previous value set for the {@code "Authorization"} header.
   *
   * @param username the username
   * @param password the password
   * @return {@code this}
   */
  default RequestSpec basicAuth(String username, String password) {
    byte[] bytes = (username + ":" + password).getBytes(StandardCharsets.ISO_8859_1);
    byte[] encodedBytes = Base64.getEncoder().encode(bytes);
    getHeaders().set(HttpHeaderNames.AUTHORIZATION, "Basic " + new String(encodedBytes, StandardCharsets.ISO_8859_1));
    return this;
  }

  /**
   * The request body.
   * <p>
   * The methods of this type are not additive.
   * That is, successive calls to {@link #bytes(byte[])} and other methods will not add to the request body.
   * Rather, they will replace the content specified by previous method calls.
   * <p>
   * It is generally best to provide the body content in the format that you have it in.
   * That is, if you already have the desired body content as a {@link String}, use the {@link #text(CharSequence)} method.
   * If you already have the desired body content as a {@code byte[]}, use the {@link #bytes(byte[])} method.
   */
  interface Body {

    /**
     * Specifies the {@code "Content-Type"} of the request.
     * <p>
     * Call this method has the same effect as using {@link #getHeaders()} or {@link #headers(ratpack.func.Action)} to set the {@code "Content-Type"} header.
     *
     * @param contentType the value of the Content-Type header
     * @return this
     */
    Body type(CharSequence contentType);

    /**
     * Specifies the request body by writing to an output stream.
     * <p>
     * The output stream is not directly connected to the HTTP server.
     * That is, bytes written to the given output stream are not directly streamed to the server.
     * There is no performance advantage in using this method over methods such as {@link #bytes(byte[])}.
     *
     * @param action an action that writes to the request body to the
     * @return this
     * @throws Exception any thrown by action
     */
    Body stream(Action<? super OutputStream> action) throws Exception;

    /**
     * Specifies the request body as a byte buffer.
     * <p>
     * The given byte buffer will not be copied.
     * That is, changes to the byte buffer made after calling this method will affect the body.
     *
     * @param byteBuf the intended request body
     * @return this
     */
    Body buffer(ByteBuf byteBuf);

    /**
     * Specifies the request body as a byte array.
     * <p>
     * The given byte array will not be copied.
     * That is, changes to the byte array made after calling this method will affect the body.
     *
     * @param bytes the intended request body
     * @return this
     */
    Body bytes(byte[] bytes);

    /**
     * Specifies the request body as a UTF-8 char sequence.
     * <p>
     * This method is a shorthand for calling {@link #text(CharSequence, java.nio.charset.Charset)} with a UTF-8 charset.
     *
     * @param text the request body
     * @return this
     * @see #text(CharSequence, java.nio.charset.Charset)
     */
    Body text(CharSequence text);

    /**
     * Specifies the request body as a char sequence of the given charset.
     * <p>
     * Unlike other methods of this interface, this method will set the request {@code "Content-Type"} header if it has not already been set.
     * If it has not been set, it will be set to {@code "text/plain;charset=«charset»"}.
     *
     * @param text the request body
     * @param charset the charset of the request body (used to convert the text to bytes)
     * @return this
     */
    Body text(CharSequence text, Charset charset);

  }

}
