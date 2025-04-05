/*
 * Copyright 2015 the original author or authors.
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

package ratpack.server.internal;

import io.netty.handler.codec.compression.Brotli;
import io.netty.handler.codec.compression.CompressionOptions;
import io.netty.handler.codec.compression.StandardCompressionOptions;
import io.netty.handler.codec.compression.Zstd;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponse;
import ratpack.http.internal.HttpHeaderConstants;

import java.util.Objects;
import java.util.stream.Stream;

public class IgnorableHttpContentCompressor extends HttpContentCompressor {

  // Define compression options for the supported encodings to fix https://github.com/netty/netty/issues/14972
  public IgnorableHttpContentCompressor() {
    super(
      Stream.of(
        Brotli.isAvailable() ? StandardCompressionOptions.brotli() : null,
        StandardCompressionOptions.deflate(),
        StandardCompressionOptions.gzip(),
        Zstd.isAvailable()
          ? StandardCompressionOptions.zstd(
          /* DEFAULT_COMPRESSION_LEVEL = */ 3,
          /* DEFAULT_BLOCK_SIZE = */ 1 << 16,
          Integer.MAX_VALUE) // default maxEncodeSize is 32MiB which is too small, as this is a hard limit let's set it to Integer.MAX_VALUE
          : null
      ).filter(Objects::nonNull).toArray(CompressionOptions[]::new)
    );
  }

  @SuppressWarnings("deprecation")
  @Override
  protected Result beginEncode(HttpResponse res, String acceptEncoding) throws Exception {
    String contentEncoding = res.headers().getAsString(HttpHeaderConstants.CONTENT_ENCODING);

    //Allow setting content encoding to identity to skip encoding this supports the Response.noCompress() call.
    if (HttpHeaderValues.IDENTITY.contentEqualsIgnoreCase(contentEncoding)) {
      // Remove header as one SHOULD NOT send Identity as content encoding.
      // This keeps with current behavior there HttpContentCompressor will leave the header in normally.
      res.headers().remove(HttpHeaderConstants.CONTENT_ENCODING);
      return null;
    }

    return super.beginEncode(res, acceptEncoding);
  }
}
