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

package ratpack.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.CharsetUtil;
import org.reactivestreams.Publisher;
import ratpack.func.Function;
import ratpack.handling.Context;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.render.Renderable;
import ratpack.stream.Streams;
import ratpack.util.Exceptions;

import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * A {@link ratpack.handling.Context#render(Object) renderable} object for streaming data with HTTP chunked transfer-encoding.
 * <p>
 * A {@link ratpack.render.Renderer renderer} for this type is implicitly provided by Ratpack core.
 * <p>
 * Example usage:
 * <pre class="java">{@code
 * import org.reactivestreams.Publisher;
 * import ratpack.stream.Streams;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import java.time.Duration;
 *
 * import static ratpack.http.ResponseChunks.stringChunks;
 *
 * import static org.junit.Assert.assertEquals;
 *
 * public class Example {
 *   public static void main(String[] args) throws Exception {
 *     EmbeddedApp.fromHandler(ctx -> {
 *       Publisher<String> strings = Streams.periodically(ctx, Duration.ofMillis(5),
 *         i -> i < 5 ? i.toString() : null
 *       );
 *
 *       ctx.render(stringChunks(strings));
 *     }).test(httpClient -> {
 *       assertEquals("01234", httpClient.getText());
 *     });
 *   }
 * }
 * }</pre>
 *
 * @see Response#sendStream(org.reactivestreams.Publisher)
 * @see <a href="http://en.wikipedia.org/wiki/Chunked_transfer_encoding" target="_blank">Wikipedia - Chunked transfer encoding</a>
 */
public class ResponseChunks implements Renderable {

  /**
   * Transmit each string emitted by the publisher as a chunk.
   * <p>
   * The content type of the response is set to {@code text/plain;charset=UTF-8} and each string is decoded as UTF-8.
   *
   * @param publisher a publisher of strings
   * @return a renderable object
   */
  public static ResponseChunks stringChunks(Publisher<? extends CharSequence> publisher) {
    return stringChunks(HttpHeaderConstants.PLAIN_TEXT_UTF8, CharsetUtil.UTF_8, publisher);
  }

  /**
   * Transmit each string emitted by the publisher as a chunk.
   * <p>
   * The content type of the response is set to the given content type and each string is decoded as UTF-8.
   *
   * @param contentType the value for the content-type header
   * @param publisher a publisher of strings
   * @return a renderable object
   */
  public static ResponseChunks stringChunks(CharSequence contentType, Publisher<? extends CharSequence> publisher) {
    return stringChunks(contentType, CharsetUtil.UTF_8, publisher);
  }

  /**
   * Transmit each string emitted by the publisher as a chunk.
   * <p>
   * The content type of the response is set to the given content type and each string is decoded as the given charset.
   *
   * @param contentType the value for the content-type header
   * @param charset the charset to use to decode each string chunk
   * @param publisher a publisher of strings
   * @return a renderable object
   */
  public static ResponseChunks stringChunks(CharSequence contentType, Charset charset, Publisher<? extends CharSequence> publisher) {
    return new ResponseChunks(contentType, allocator ->
      Streams.map(publisher, charSequence ->
          ByteBufUtil.encodeString(allocator, CharBuffer.wrap(charSequence), charset)
      )
    );
  }

  /**
   * Transmit each set of bytes emitted by the publisher as a chunk.
   * <p>
   * The content type of the response is set to the given content type.
   *
   * @param contentType the value for the content-type header
   * @param publisher a publisher of byte buffers
   * @return a renderable object
   */
  public static ResponseChunks bufferChunks(CharSequence contentType, Publisher<? extends ByteBuf> publisher) {
    return new ResponseChunks(contentType, byteBufAllocator -> publisher);
  }

  private final Function<? super ByteBufAllocator, ? extends Publisher<? extends ByteBuf>> publisherFactory;
  private final CharSequence contentType;

  private ResponseChunks(CharSequence contentType, Function<? super ByteBufAllocator, ? extends Publisher<? extends ByteBuf>> publisherFactory) {
    this.publisherFactory = publisherFactory;
    this.contentType = contentType;
  }

  /**
   * Returns the chunk publisher.
   * <p>
   * This method is called internally by the renderer for this type.
   *
   * @param byteBufAllocator a byte buf allocator that can be used if necessary to allocate byte buffers
   * @return a publisher of byte buffers
   */
  public Publisher<? extends ByteBuf> publisher(ByteBufAllocator byteBufAllocator) {
    return Exceptions.uncheck(() -> publisherFactory.apply(byteBufAllocator));
  }

  /**
   * The intended value of the content-type header.
   *
   * @return the intended value of the content-type header.
   */
  public CharSequence getContentType() {
    return contentType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void render(Context context) throws Exception {
    Response response = context.getResponse();
    response.getHeaders().add(HttpHeaderConstants.TRANSFER_ENCODING, HttpHeaderConstants.CHUNKED);
    response.getHeaders().set(HttpHeaderConstants.CONTENT_TYPE, getContentType());
    Publisher<? extends ByteBuf> publisher = publisher(context.get(ByteBufAllocator.class));
    response.sendStream(publisher);
  }

}
