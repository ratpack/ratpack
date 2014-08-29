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

package ratpack.http.stream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.CharsetUtil;
import org.reactivestreams.Publisher;
import ratpack.func.Function;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.stream.Streams;

import java.nio.CharBuffer;
import java.nio.charset.Charset;

// TODO docs and examples here
public class HttpResponseChunks {

  public static HttpResponseChunks stringChunks(final Publisher<? extends CharSequence> publisher) {
    return stringChunks(HttpHeaderConstants.UTF_8_TEXT, CharsetUtil.UTF_8, publisher);
  }

  public static HttpResponseChunks stringChunks(CharSequence contentType, final Publisher<? extends CharSequence> publisher) {
    return stringChunks(contentType, CharsetUtil.UTF_8, publisher);
  }

  public static HttpResponseChunks stringChunks(CharSequence contentType, final Charset charset, final Publisher<? extends CharSequence> publisher) {
    return new HttpResponseChunks(contentType, new Function<ByteBufAllocator, Publisher<? extends ByteBuf>>() {
      @Override
      public Publisher<? extends ByteBuf> apply(final ByteBufAllocator byteBufAllocator) throws Exception {
        return Streams.transform(publisher, new Function<CharSequence, ByteBuf>() {
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

  public static HttpResponseChunks bufferChunks(CharSequence contentType, final Publisher<? extends ByteBuf> publisher) {
    return new HttpResponseChunks(contentType, new Function<ByteBufAllocator, Publisher<? extends ByteBuf>>() {
      @Override
      public Publisher<? extends ByteBuf> apply(ByteBufAllocator byteBufAllocator) throws Exception {
        return publisher;
      }
    });
  }

  private final Function<? super ByteBufAllocator, ? extends Publisher<? extends ByteBuf>> publisherFactory;
  private final CharSequence contentType;

  private HttpResponseChunks(CharSequence contentType, Function<? super ByteBufAllocator, ? extends Publisher<? extends ByteBuf>> publisherFactory) {
    this.publisherFactory = publisherFactory;
    this.contentType = contentType;
  }

  public Publisher<? extends ByteBuf> publisher(ByteBufAllocator byteBufAllocator) throws Exception {
    return publisherFactory.apply(byteBufAllocator);
  }

  public CharSequence getContentType() {
    return contentType;
  }

}
