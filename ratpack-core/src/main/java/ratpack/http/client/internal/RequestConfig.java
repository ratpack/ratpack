/*
 * Copyright 2016 the original author or authors.
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

package ratpack.http.client.internal;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.util.CharsetUtil;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.http.HttpMethod;
import ratpack.http.MutableHeaders;
import ratpack.http.client.HttpClient;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.http.internal.NettyHeadersBackedMutableHeaders;

import javax.net.ssl.SSLContext;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Duration;

class RequestConfig {

  final URI uri;
  final HttpMethod method;
  final MutableHeaders headers;
  final ByteBuf body;
  final int maxContentLength;
  final Duration connectTimeout;
  final Duration readTimeout;
  final boolean decompressResponse;
  final int maxRedirects;
  final SSLContext sslContext;
  final Function<? super ReceivedResponse, Action<? super RequestSpec>> onRedirect;

  static RequestConfig of(URI uri, HttpClient httpClient, Action<? super RequestSpec> action) throws Exception {
    Spec spec = new Spec(uri, httpClient.getByteBufAllocator());

    spec.readTimeout = httpClient.getReadTimeout();
    spec.maxContentLength = httpClient.getMaxContentLength();

    try {
      action.execute(spec);
    } catch (Exception any) {
      if (spec.bodyByteBuf != null) {
        spec.bodyByteBuf.release();
      }
      throw any;
    }

    return new RequestConfig(
      spec.uri,
      spec.method,
      spec.headers,
      spec.bodyByteBuf,
      spec.maxContentLength,
      spec.connectTimeout,
      spec.readTimeout,
      spec.decompressResponse,
      spec.maxRedirects,
      spec.sslContext,
      spec.onRedirect
    );
  }

  private RequestConfig(URI uri, HttpMethod method, MutableHeaders headers, ByteBuf body, int maxContentLength, Duration connectTimeout, Duration readTimeout, boolean decompressResponse, int maxRedirects, SSLContext sslContext, Function<? super ReceivedResponse, Action<? super RequestSpec>> onRedirect) {
    this.uri = uri;
    this.method = method;
    this.headers = headers;
    this.body = body;
    this.maxContentLength = maxContentLength;
    this.connectTimeout = connectTimeout;
    this.readTimeout = readTimeout;
    this.decompressResponse = decompressResponse;
    this.maxRedirects = maxRedirects;
    this.sslContext = sslContext;
    this.onRedirect = onRedirect;
  }

  private static class Spec implements RequestSpec {

    private final ByteBufAllocator byteBufAllocator;
    private final URI uri;

    private MutableHeaders headers = new NettyHeadersBackedMutableHeaders(new DefaultHttpHeaders());
    private boolean decompressResponse = true;
    private Duration connectTimeout = Duration.ofSeconds(30);
    private Duration readTimeout = Duration.ofSeconds(30);
    private int maxContentLength = -1;
    private ByteBuf bodyByteBuf = Unpooled.EMPTY_BUFFER;
    private HttpMethod method = HttpMethod.GET;
    private int maxRedirects = RequestSpec.DEFAULT_MAX_REDIRECTS;
    private SSLContext sslContext;
    private Function<? super ReceivedResponse, Action<? super RequestSpec>> onRedirect;
    private BodyImpl body = new BodyImpl();

    Spec(URI uri, ByteBufAllocator byteBufAllocator) {
      this.uri = uri;
      this.byteBufAllocator = byteBufAllocator;
    }

    @Override
    public RequestSpec onRedirect(Function<? super ReceivedResponse, Action<? super RequestSpec>> function) {
      this.onRedirect = function;
      return this;
    }

    @Override
    public RequestSpec redirects(int maxRedirects) {
      Preconditions.checkArgument(maxRedirects >= 0);
      this.maxRedirects = maxRedirects;
      return this;
    }

    @Override
    public int getRedirects() {
      return this.maxRedirects;
    }

    @Override
    public RequestSpec sslContext(SSLContext sslContext) {
      this.sslContext = sslContext;
      return this;
    }

    @Override
    public SSLContext getSslContext() {
      return this.sslContext;
    }

    @Override
    public MutableHeaders getHeaders() {
      return headers;
    }

    @Override
    public RequestSpec maxContentLength(int numBytes) {
      this.maxContentLength = numBytes;
      return this;
    }

    @Override
    public int getMaxContentLength() {
      return this.maxContentLength;
    }

    @Override
    public RequestSpec headers(Action<? super MutableHeaders> action) throws Exception {
      action.execute(getHeaders());
      return this;
    }

    @Override
    public RequestSpec method(HttpMethod method) {
      this.method = method;
      return this;
    }

    @Override
    public HttpMethod getMethod() {
      return this.method;
    }

    @Override
    public URI getUri() {
      return uri;
    }

    @Override
    public RequestSpec decompressResponse(boolean shouldDecompress) {
      this.decompressResponse = shouldDecompress;
      return this;
    }

    @Override
    public boolean getDecompressResponse() {
      return this.decompressResponse;
    }

    @Override
    public RequestSpec connectTimeout(Duration duration) {
      this.connectTimeout = duration;
      return this;
    }

    @Override
    public Duration getConnectTimeout() {
      return this.connectTimeout;
    }

    @Override
    public RequestSpec readTimeout(Duration duration) {
      this.readTimeout = duration;
      return this;
    }

    @Override
    public Duration getReadTimeout() {
      return this.readTimeout;
    }

    private void setBodyByteBuf(ByteBuf byteBuf) {
      if (bodyByteBuf != null) {
        bodyByteBuf.release();
      }
      bodyByteBuf = byteBuf.touch();
    }


    private class BodyImpl implements Body {
      @Override
      public Body type(String contentType) {
        getHeaders().set(HttpHeaderConstants.CONTENT_TYPE, contentType);
        return this;
      }

      @Override
      public Body stream(Action<? super OutputStream> action) throws Exception {
        ByteBuf byteBuf = byteBufAllocator.buffer();
        try (OutputStream outputStream = new ByteBufOutputStream(byteBuf)) {
          action.execute(outputStream);
        } catch (Throwable t) {
          byteBuf.release();
          throw t;
        }

        setBodyByteBuf(byteBuf);
        return this;
      }

      @Override
      public Body buffer(ByteBuf byteBuf) {
        setBodyByteBuf(byteBuf);
        return this;
      }

      @Override
      public Body bytes(byte[] bytes) {
        setBodyByteBuf(Unpooled.wrappedBuffer(bytes));
        return this;
      }

      @Override
      public Body text(CharSequence text) {
        return text(text, CharsetUtil.UTF_8);
      }

      @Override
      public Body text(CharSequence text, Charset charset) {
        if (charset.equals(CharsetUtil.UTF_8)) {
          maybeSetContentType(HttpHeaderConstants.PLAIN_TEXT_UTF8);
        } else {
          maybeSetContentType("text/plain;charset=" + charset.name());
        }
        setBodyByteBuf(Unpooled.copiedBuffer(text, charset));
        return this;
      }

      private void maybeSetContentType(CharSequence s) {
        if (!headers.contains(HttpHeaderConstants.CONTENT_TYPE.toString())) {
          headers.set(HttpHeaderConstants.CONTENT_TYPE, s);
        }
      }
    }

    @Override
    public Body getBody() {
      return body;
    }

    @Override
    public RequestSpec body(Action<? super Body> action) throws Exception {
      action.execute(getBody());
      return this;
    }
  }
}
