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
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.stream.Streams;
import ratpack.util.ExceptionUtils;

import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * A {@link ratpack.handling.Context#render(Object) renderable} object for streaming data with HTTP chunked transfer-encoding.
 * <p>
 * A {@link ratpack.render.Renderer renderer} for this type is implicitly provided by Ratpack core.
 * <p>
 * Example usage:
 * <pre class="java">
 * import ratpack.handling.Handler;
 * import ratpack.handling.Context;
 * import ratpack.func.Function;
 * import ratpack.stream.Streams;
 * import ratpack.launch.HandlerFactory;
 * import ratpack.launch.LaunchConfig;
 * import ratpack.launch.LaunchConfigBuilder;
 * import ratpack.test.embed.EmbeddedApplication;
 * import ratpack.test.embed.LaunchConfigEmbeddedApplication;
 * import ratpack.test.http.TestHttpClient;
 * import ratpack.test.http.TestHttpClients;
 * import static ratpack.http.ResponseChunks.stringChunks;
 *
 * import java.util.concurrent.TimeUnit;
 * import java.util.concurrent.ScheduledExecutorService;
 * import org.reactivestreams.Publisher;
 *
 * public class Example {
 *
 *   private static EmbeddedApplication createApp() {
 *     return new LaunchConfigEmbeddedApplication() {
 *       protected LaunchConfig createLaunchConfig() {
 *         return LaunchConfigBuilder.noBaseDir().port(0).build(new HandlerFactory() {
 *             public Handler create(LaunchConfig launchConfig) {
 *
 *               // Example of streaming chunks
 *
 *               return new Handler() {
 *                 public void handle(Context context) {
 *                   // simulate streaming by periodically publishing
 *                   ScheduledExecutorService executor = context.getLaunchConfig().getExecController().getExecutor();
 *                   Publisher&lt;String&gt; strings = Streams.periodically(executor, 5, TimeUnit.MILLISECONDS, new Function&lt;Integer, String&gt;() {
 *                     public String apply(Integer i) {
 *                       if (i.intValue() &lt; 5) {
 *                         return i.toString();
 *                       } else {
 *                         return null;
 *                       }
 *                     }
 *                   });
 *
 *                   context.render(stringChunks(strings));
 *                 }
 *               };
 *
 *             }
 *           });
 *       }
 *     };
 *   }
 *
 *   public static void main(String[] args) {
 *     try(EmbeddedApplication app = createApp()) {
 *       assert app.getHttpClient().getText().equals("01234");
 *     }
 *   }
 *
 * }
 * </pre>
 *
 * @see Response#sendStream(org.reactivestreams.Publisher)
 * @see <a href="http://en.wikipedia.org/wiki/Chunked_transfer_encoding" target="_blank">Wikipedia - Chunked transfer encoding</a>
 */
public class ResponseChunks {

  /**
   * Transmit each string emitted by the publisher as a chunk.
   * <p>
   * The content type of the response is set to {@code text/plain;charset=UTF-8} and each string is decoded as UTF-8.
   *
   * @param publisher a publisher of strings
   * @return a renderable object
   */
  public static ResponseChunks stringChunks(final Publisher<? extends CharSequence> publisher) {
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
  public static ResponseChunks stringChunks(CharSequence contentType, final Publisher<? extends CharSequence> publisher) {
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
  public static ResponseChunks stringChunks(CharSequence contentType, final Charset charset, final Publisher<? extends CharSequence> publisher) {
    return new ResponseChunks(contentType, new Function<ByteBufAllocator, Publisher<? extends ByteBuf>>() {
      @Override
      public Publisher<? extends ByteBuf> apply(final ByteBufAllocator byteBufAllocator) throws Exception {
        return Streams.map(publisher, new Function<CharSequence, ByteBuf>() {
          @Override
          public ByteBuf apply(CharSequence charSequence) throws Exception {
            // We are counting on Netty releasing these buffers when it sends them out.
            // this isn't going to happen if they never make it to Netty.
            // Might have to use Unpooled here
            return ByteBufUtil.encodeString(byteBufAllocator, CharBuffer.wrap(charSequence), charset);
          }
        });
      }
    });
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
  public static ResponseChunks bufferChunks(CharSequence contentType, final Publisher<? extends ByteBuf> publisher) {
    return new ResponseChunks(contentType, new Function<ByteBufAllocator, Publisher<? extends ByteBuf>>() {
      @Override
      public Publisher<? extends ByteBuf> apply(ByteBufAllocator byteBufAllocator) throws Exception {
        return publisher;
      }
    });
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
    try {
      return publisherFactory.apply(byteBufAllocator);
    } catch (Exception e) {
      // really shouldn't happen, so just uncheck and throw
      throw ExceptionUtils.uncheck(e);
    }
  }

  /**
   * The intended value of the content-type header.
   *
   * @return the intended value of the content-type header.
   */
  public CharSequence getContentType() {
    return contentType;
  }

}
