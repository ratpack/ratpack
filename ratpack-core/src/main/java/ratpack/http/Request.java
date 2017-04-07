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

import com.google.common.net.HostAndPort;
import com.google.common.reflect.TypeToken;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.cookie.Cookie;
import org.reactivestreams.Publisher;
import ratpack.api.Nullable;
import ratpack.exec.Blocking;
import ratpack.exec.Promise;
import ratpack.func.Block;
import ratpack.registry.MutableRegistry;
import ratpack.server.ServerConfig;
import ratpack.server.ServerConfigBuilder;
import ratpack.stream.Streams;
import ratpack.stream.TransformablePublisher;
import ratpack.util.MultiValueMap;
import ratpack.util.Types;

import javax.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A request to be handled.
 */
public interface Request extends MutableRegistry {

  /**
   * A type token for this type.
   *
   * @since 1.1
   */
  TypeToken<Request> TYPE = Types.token(Request.class);

  /**
   * The method of the request.
   *
   * @return The method of the request.
   */
  HttpMethod getMethod();

  /**
   * The HTTP protocol of the request.
   * <p>
   * e.g. {@code "HTTP/1.1}
   *
   *
   * @return The HTTP protocol of the request.
   */
  String getProtocol();

  /**
   * The raw URI of the request.
   * <p>
   * This value may be an absolute URI or an absolute path.
   *
   * @return The raw URI of the request.
   */
  String getRawUri();

  /**
   * The complete URI of the request (path + query string).
   * <p>
   * This value is always absolute (i.e. begins with "{@code /}").
   *
   * @return The complete URI of the request (path + query string).
   */
  String getUri();

  /**
   * The query string component of the request URI, without the "?".
   * <p>
   * If the request does not contain a query component, an empty string will be returned.
   *
   * @return The query string component of the request URI, without the "?".
   */
  String getQuery();

  /**
   * The URI without the query string and leading forward slash.
   *
   * @return The URI without the query string and leading forward slash
   */
  String getPath();

  /**
   * TBD.
   *
   * @return TBD.
   */
  MultiValueMap<String, String> getQueryParams();

  /**
   * The cookies that were sent with the request.
   * <p>
   * An empty set will be returned if no cookies were sent.
   *
   * @return The cookies that were sent with the request.
   */
  Set<Cookie> getCookies();

  /**
   * Returns the value of the cookie with the specified name if it was sent.
   * <p>
   * If there is more than one cookie with this name, this method will throw an exception.
   *
   * @param name The name of the cookie to get the value of
   * @return The cookie value, or null if not present
   */
  @Nullable
  String oneCookie(String name);

  /**
   * The body of the request.
   * <p>
   * If this request does not have a body, a non null object is still returned but it effectively has no data.
   * <p>
   * If the transmitted content is larger than provided {@link ServerConfig#getMaxContentLength()}, the given block will be invoked.
   * If the block completes successfully, the promise will be terminated.
   * If the block errors, the promise will carry the failure.
   * <p>
   * If the body is larger then {@link #getMaxContentLength()}, a {@link RequestBodyTooLargeException} will be propagated.
   *
   * @return the body of the request
   */
  Promise<TypedData> getBody();

  /**
   * Overrides the idle timeout for this connection.
   * <p>
   * The default is set as part of server config via {@link ServerConfigBuilder#idleTimeout(Duration)}.
   * <p>
   * The override strictly applies to only the request/response exchange that this request is involved in.
   *
   * @param idleTimeout the idle timeout ({@link Duration#ZERO} = no timeout, must not be negative, must not be null)
   * @since 1.5
   * @see ServerConfig#getIdleTimeout()
   */
  void setIdleTimeout(Duration idleTimeout);

  /**
   * The body of the request.
   * <p>
   * If this request does not have a body, a non null object is still returned but it effectively has no data.
   * <p>
   * If the transmitted content is larger than provided {@code maxContentLength}, the given block will be invoked.
   * If the block completes successfully, the promise will be terminated.
   * If the block errors, the promise will carry the failure.
   *
   * @param onTooLarge the action to take if the request body exceeds the given maxContentLength
   * @return the body of the request
   * @since 1.1
   */
  Promise<TypedData> getBody(Block onTooLarge);

  /**
   * The body of the request allowing up to the provided size for the content.
   * <p>
   * If this request does not have a body, a non null object is still returned but it effectively has no data.
   * <p>
   * If the transmitted content is larger than the provided {@code maxContentLength}, an {@code 413} client error will be issued.
   *
   * @param maxContentLength the maximum number of bytes allowed for the request.
   * @return the body of the request.
   * @since 1.1
   */
  Promise<TypedData> getBody(long maxContentLength);

  /**
   * The body of the request allowing up to the provided size for the content.
   * <p>
   * If this request does not have a body, a non null object is still returned but it effectively has no data.
   * <p>
   * If the transmitted content is larger than the provided {@code maxContentLength}, the given block will be invoked.
   * If the block completes successfully, the promise will be terminated.
   * If the block errors, the promise will carry the failure.
   *
   * @param maxContentLength the maximum number of bytes allowed for the request.
   * @param onTooLarge the action to take if the request body exceeds the given maxContentLength
   * @return the body of the request.
   * @since 1.1
   */
  Promise<TypedData> getBody(long maxContentLength, Block onTooLarge);

  /**
   * Allows reading the body as a stream, with back pressure.
   * <p>
   * Similar to {@link #getBodyStream(long)}, except uses {@link ServerConfig#getMaxContentLength()} as the max content length.
   *
   * @return a publisher of the request body
   * @see #getBodyStream(long)
   * @since 1.2
   */
  TransformablePublisher<? extends ByteBuf> getBodyStream();

  /**
   * Allows reading the body as a stream, with back pressure.
   * <p>
   * The returned publisher emits the body as {@link ByteBuf}s.
   * The subscriber <b>MUST</b> {@code release()} each emitted byte buf.
   * Failing to do so will leak memory.
   * <p>
   * If the request body is larger than the given {@code maxContentLength}, a {@link RequestBodyTooLargeException} will be emitted.
   * If the request body has already been read, a {@link RequestBodyAlreadyReadException} will be emitted.
   * <p>
   * The returned publisher is bound to the calling execution via {@link Streams#bindExec(Publisher)}.
   * If your subscriber's onNext(), onComplete() or onError() methods are asynchronous they <b>MUST</b> use {@link Promise},
   * {@link Blocking} or similar execution control constructs.
   * <p>
   * The following demonstrates how to use this method to stream the request body to a file, using asynchronous IO.
   *
   * <pre class="java">
   * import com.google.common.io.Files;
   * import io.netty.buffer.ByteBuf;
   * import org.apache.commons.lang3.RandomStringUtils;
   * import org.reactivestreams.Subscriber;
   * import org.reactivestreams.Subscription;
   * import org.junit.Assert;
   * import ratpack.exec.Promise;
   * import ratpack.http.client.ReceivedResponse;
   * import ratpack.test.embed.EmbeddedApp;
   * import ratpack.test.embed.EphemeralBaseDir;
   *
   * import java.io.IOException;
   * import java.nio.channels.AsynchronousFileChannel;
   * import java.nio.charset.StandardCharsets;
   * import java.nio.file.Path;
   * import java.nio.file.StandardOpenOption;
   *
   * public class Example {
   *   public static void main(String[] args) throws Exception {
   *     EphemeralBaseDir.tmpDir().use(dir -> {
   *       String string = RandomStringUtils.random(1024 * 1024);
   *       int length = string.getBytes(StandardCharsets.UTF_8).length;
   *
   *       Path file = dir.path("out.txt");
   *
   *       EmbeddedApp.fromHandler(ctx ->
   *         ctx.getRequest().getBodyStream(length).subscribe(new Subscriber<ByteBuf>() {
   *
   *           private Subscription subscription;
   *           private AsynchronousFileChannel out;
   *           long written;
   *
   *           {@literal @}Override
   *           public void onSubscribe(Subscription s) {
   *             subscription = s;
   *             try {
   *               this.out = AsynchronousFileChannel.open(
   *                 file, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
   *               );
   *               subscription.request(1);
   *             } catch (IOException e) {
   *               subscription.cancel();
   *               ctx.error(e);
   *             }
   *           }
   *
   *           {@literal @}Override
   *           public void onNext(ByteBuf byteBuf) {
   *             Promise.<Integer>async(down ->
   *               out.write(byteBuf.nioBuffer(), written, null, down.completionHandler())
   *             ).onError(error -> {
   *               byteBuf.release();
   *               subscription.cancel();
   *               out.close();
   *               ctx.error(error);
   *             }).then(bytesWritten -> {
   *               byteBuf.release();
   *               written += bytesWritten;
   *               subscription.request(1);
   *             });
   *           }
   *
   *           {@literal @}Override
   *           public void onError(Throwable t) {
   *             ctx.error(t);
   *             try {
   *               out.close();
   *             } catch (IOException ignore) {
   *               // ignore
   *             }
   *           }
   *
   *           {@literal @}Override
   *           public void onComplete() {
   *             try {
   *               out.close();
   *             } catch (IOException ignore) {
   *               // ignore
   *             }
   *             ctx.render("ok");
   *           }
   *         })
   *       ).test(http -> {
   *         ReceivedResponse response = http.request(requestSpec -> requestSpec.method("POST").getBody().text(string));
   *         Assert.assertEquals(response.getBody().getText(), "ok");
   *
   *         String fileContents = Files.toString(file.toFile(), StandardCharsets.UTF_8);
   *         Assert.assertEquals(fileContents, string);
   *       });
   *     });
   *   }
   * }
   * </pre>
   *
   * @return a publisher of the request body
   * @see #getBodyStream(long)
   * @since 1.2
   */
  TransformablePublisher<? extends ByteBuf> getBodyStream(long maxContentLength);

  /**
   * The request headers.
   *
   * @return The request headers.
   */
  Headers getHeaders();

  /**
   * The type of the data as specified in the {@code "content-type"} header.
   * <p>
   * If no {@code "content-type"} header is specified, an empty {@link MediaType} is returned.
   *
   * @return The type of the data.
   * @see ratpack.http.MediaType#isEmpty()
   */
  MediaType getContentType();

  /**
   * The address of the client that initiated the request.
   *
   * @return the address of the client that initiated the request
   */
  HostAndPort getRemoteAddress();

  /**
   * The address of the local network interface that received the request.
   *
   * @return the address of the network interface that received the request
   */
  HostAndPort getLocalAddress();

  /**
   * A flag representing whether or not the request originated via AJAX.
   * @return A flag representing whether or not the request originated via AJAX.
   */
  boolean isAjaxRequest();

  /**
   * Whether this request contains a {@code Expect: 100-Continue} header.
   * <p>
   * You do not need to send the {@code 100 Continue} status response.
   * It will be automatically sent when the body is read via {@link #getBody()} or {@link #getBodyStream(long)} (or variants).
   * Ratpack will not emit a {@code 417 Expectation Failed} response if the body is not read.
   * The response specified by the handling code will not be altered, as per normal.
   * <p>
   * If the header is present for the request, this method will return {@code true} before and after the {@code 100 Continue} response has been sent.
   *
   * @return whether this request includes the {@code Expect: 100-Continue} header
   * @since 1.2
   */
  boolean isExpectsContinue();

  /**
   * Whether this request contains a {@code Transfer-Encoding: chunked} header.
   * <p>
   * See <a href="https://en.wikipedia.org/wiki/Chunked_transfer_encoding">https://en.wikipedia.org/wiki/Chunked_transfer_encoding</a>.
   *
   * @return whether this request contains a {@code Transfer-Encoding: chunked} header
   * @since 1.2
   */
  boolean isChunkedTransfer();

  /**
   * The advertised content length of the request body.
   * <p>
   * Will return {@code -1} if no {@code Content-Length} header is present, or is not valid long value.
   *
   * @return the advertised content length of the request body.
   * @since 1.2
   */
  long getContentLength();

  /**
   * The timestamp for when this request was received.
   * Specifically, this is the timestamp of creation of the request object.
   *
   * @return the instant timestamp for the request.
   */
  Instant getTimestamp();

  /**
   * Sets the allowed max content length for the request body.
   * <p>
   * This setting will be used when {@link #getBody()} or {@link #getBodyStream()} are called,
   * and when implicitly reading the request body in order to respond (e.g. when issuing a response without trying to read the body).
   *
   * @param maxContentLength the maximum request body length in bytes
   * @since 1.5
   */
  void setMaxContentLength(long maxContentLength);

  /**
   * The max allowed content length for the request body.
   *
   * @see #setMaxContentLength(long)
   * @return the maximum request body length in bytes
   * @since 1.5
   */
  long getMaxContentLength();

  /**
   * The client's verified certificate if the connection was made with HTTPS
   * and client authentication is enabled.
   *
   * @return the client's certificate
   * @since 1.5
   */
  Optional<X509Certificate> getClientCertificate();

  /**
   * {@inheritDoc}
   */
  @Override
  <O> Request add(Class<O> type, O object);

  /**
   * {@inheritDoc}
   */
  @Override
  <O> Request add(TypeToken<O> type, O object);

  /**
   * {@inheritDoc}
   */
  @Override
  Request add(Object object);

  /**
   * {@inheritDoc}
   */
  @Override
  <O> Request addLazy(Class<O> type, Supplier<? extends O> supplier);

  /**
   * {@inheritDoc}
   */
  @Override
  <O> Request addLazy(TypeToken<O> type, Supplier<? extends O> supplier);

}
