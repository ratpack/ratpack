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

package ratpack.server.internal;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import ratpack.http.internal.HttpHeaderConstants;

import java.util.List;

/**
 * Adapted from https://github.com/scireum/sirius/blob/develop/web/src/sirius/web/http/SmartHttpContentCompressor.java
 *
 * Credit to Andreas Haufler (aha@scireum.de).
 */
public class SmartHttpContentCompressor extends HttpContentCompressor {

  private boolean passThrough;

  @Override
  protected void encode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out) throws Exception {
    if (msg instanceof HttpResponse) {
      // Check if this response should be compressed
      HttpResponse res = (HttpResponse) msg;
      // by default compression is on (passThrough bypasses compression)
      passThrough = false;
      // If an "Content-Encoding: Identity" header was set, we do not compress
      if (res.headers().contains(HttpHeaderConstants.CONTENT_ENCODING, HttpHeaderConstants.IDENTITY, false)) {
        passThrough = true;
        // Remove header as one SHOULD NOT send Identity as content encoding.
        res.headers().remove(HttpHeaderConstants.CONTENT_ENCODING);
      }
    }
    super.encode(ctx, msg, out);
  }

  @Override
  protected Result beginEncode(HttpResponse headers, String acceptEncoding) throws Exception {
    // If compression is skipped, we return null here which disables the compression effectively...
    if (passThrough) {
      return null;
    }
    return super.beginEncode(headers, acceptEncoding);
  }
}
