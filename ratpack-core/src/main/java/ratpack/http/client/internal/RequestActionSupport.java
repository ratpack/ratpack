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

package ratpack.http.client.internal;

import com.google.common.net.HostAndPort;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.PrematureChannelClosureException;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import ratpack.exec.Downstream;
import ratpack.exec.Execution;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.http.Headers;
import ratpack.http.MutableHeaders;
import ratpack.http.Status;
import ratpack.http.client.HttpClientRequestInterceptor;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.http.internal.*;
import ratpack.util.internal.ChannelImplDetector;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static ratpack.util.Exceptions.uncheck;

abstract class RequestActionSupport<T> implements RequestAction<T> {

  private static final Pattern ABSOLUTE_PATTERN = Pattern.compile("^https?://.*");

  private final Action<? super RequestSpec> requestConfigurer;
  private final boolean finalUseSsl;
  private final String host;
  private final int port;
  private final MutableHeaders headers;
  private final RequestSpecBacking requestSpecBacking;
  private final URI uri;
  private final int redirectCounter;
  private final RequestParams requestParams;
  private final AtomicBoolean fired = new AtomicBoolean();

  protected final Execution execution;
  protected final ByteBufAllocator byteBufAllocator;

  private final Optional<HttpClientRequestInterceptor> requestInterceptor;

  public RequestActionSupport(Action<? super RequestSpec> requestConfigurer,
                              URI uri,
                              Execution execution,
                              ByteBufAllocator byteBufAllocator,
                              int redirectCounter) {
    this(requestConfigurer, uri, execution, byteBufAllocator, redirectCounter, Optional.empty());
  }

  public RequestActionSupport(Action<? super RequestSpec> requestConfigurer,
                              URI uri,
                              Execution execution,
                              ByteBufAllocator byteBufAllocator,
                              int redirectCounter,
                              Optional<HttpClientRequestInterceptor> requestInterceptor) {
    this.execution = execution;
    this.requestConfigurer = requestConfigurer;
    this.byteBufAllocator = byteBufAllocator;
    this.uri = uri;
    this.redirectCounter = redirectCounter;
    this.requestParams = new RequestParams();
    this.headers = new NettyHeadersBackedMutableHeaders(new DefaultHttpHeaders());
    this.requestSpecBacking = new RequestSpecBacking(headers, uri, byteBufAllocator, requestParams);
    this.requestInterceptor = requestInterceptor;

    try {
      requestConfigurer.execute(requestSpecBacking.asSpec());
    } catch (Exception e) {
      throw uncheck(e);
    }

    String scheme = uri.getScheme();
    boolean useSsl = false;
    if (scheme.equals("https")) {
      useSsl = true;
    } else if (!scheme.equals("http")) {
      throw new IllegalArgumentException(String.format("URL '%s' is not a http url", uri.toString()));
    }
    this.finalUseSsl = useSsl;

    this.host = uri.getHost();
    this.port = uri.getPort() < 0 ? (useSsl ? 443 : 80) : uri.getPort();
  }

  private static ByteBuf initBufferReleaseOnExecutionClose(final ByteBuf responseBuffer, Execution execution) {
    execution.onComplete(responseBuffer::release);
    return responseBuffer;
  }

  public void connect(final Downstream<? super T> downstream) throws Exception {
    final Bootstrap b = new Bootstrap();
    b.group(this.execution.getEventLoop())
      .channel(ChannelImplDetector.getSocketChannelImpl())
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) requestParams.connectTimeout.toMillis())
      .handler(new ChannelInitializer<SocketChannel>() {
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
          ChannelPipeline p = ch.pipeline();

          if (finalUseSsl) {
            SSLEngine sslEngine;
            if (requestSpecBacking.getSslContext() != null) {
              sslEngine = requestSpecBacking.getSslContext().createSSLEngine();
            } else {
              sslEngine = SSLContext.getDefault().createSSLEngine();
            }
            sslEngine.setUseClientMode(true);
            p.addLast("ssl", new SslHandler(sslEngine));
          }

          p.addLast("codec", new HttpClientCodec());
          p.addLast("readTimeout", new ReadTimeoutHandler(requestParams.readTimeoutNanos, TimeUnit.NANOSECONDS));

          p.addLast("redirectHandler", new SimpleChannelInboundHandler<HttpObject>(false) {
            boolean readComplete;
            boolean redirected;

            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
              if (!readComplete) {
                error(downstream, new PrematureChannelClosureException("Server " + uri + " closed the connection prematurely"));
              }
              super.channelReadComplete(ctx);
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
              super.exceptionCaught(ctx, cause);
            }

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
              if (msg instanceof HttpResponse) {
                readComplete = true;
                final HttpResponse response = (HttpResponse) msg;
                int maxRedirects = requestSpecBacking.getMaxRedirects();
                int status = response.status().code();
                String locationValue = response.headers().getAsString(HttpHeaderConstants.LOCATION);

                Action<? super RequestSpec> redirectConfigurer = RequestActionSupport.this.requestConfigurer;
                if (isRedirect(status) && redirectCounter < maxRedirects && locationValue != null) {
                  final Function<? super ReceivedResponse, Action<? super RequestSpec>> onRedirect = requestSpecBacking.getOnRedirect();
                  if (onRedirect != null) {
                    final Action<? super RequestSpec> onRedirectResult = onRedirect.apply(toReceivedResponse(response));
                    if (onRedirectResult == null) {
                      redirectConfigurer = null;
                    } else {
                      redirectConfigurer = redirectConfigurer.append(onRedirectResult);
                    }
                  }

                  if (redirectConfigurer != null) {
                    Action<? super RequestSpec> redirectRequestConfig = s -> {
                      if (status == 301 || status == 302) {
                        s.method("GET");
                      }
                    };
                    redirectRequestConfig = redirectRequestConfig.append(redirectConfigurer);

                    URI locationUrl;
                    if (ABSOLUTE_PATTERN.matcher(locationValue).matches()) {
                      locationUrl = new URI(locationValue);
                    } else {
                      locationUrl = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), locationValue, null, null);
                    }

                    buildRedirectRequestAction(redirectRequestConfig, locationUrl, redirectCounter + 1).connect(downstream);
                    redirected = true;
                  }
                }
              }

              if (!redirected) {
                ctx.fireChannelRead(msg);
              }
            }
          });

          if (requestSpecBacking.isDecompressResponse()) {
            p.addLast(new HttpContentDecompressor());
          }
          addResponseHandlers(p, downstream);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
          ctx.close();
          error(downstream, cause);
        }
      });

    ChannelFuture connectFuture = b.connect(host, port);
    connectFuture.addListener(f1 -> {
      if (connectFuture.isSuccess()) {
        String fullPath = getFullPath(uri);
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.valueOf(requestSpecBacking.getMethod()), fullPath, requestSpecBacking.getBody());

        if (headers.get(HttpHeaderConstants.HOST) == null) {
          HostAndPort hostAndPort = HostAndPort.fromParts(host, port);
          headers.set(HttpHeaderConstants.HOST, hostAndPort.toString());
        }
        headers.set(HttpHeaderConstants.CONNECTION, HttpHeaderValues.CLOSE);
        int contentLength = request.content().readableBytes();
        if (contentLength > 0) {
          headers.set(HttpHeaderConstants.CONTENT_LENGTH, Integer.toString(contentLength));
        }

        HttpHeaders requestHeaders = request.headers();
        requestHeaders.set(headers.getNettyHeaders());

        ChannelFuture writeFuture = connectFuture.channel().writeAndFlush(request);
        writeFuture.addListener(f2 -> {
          //invoke the request interceptor
          requestInterceptor
            .ifPresent(c -> c.intercept(new ImmutableSentRequest(request.method().name(),
              request.uri(),
              new NettyHeadersBackedHeaders(requestHeaders))));
          if (!writeFuture.isSuccess()) {
            writeFuture.channel().close();
            error(downstream, writeFuture.cause());
          }
        });
      } else {
        connectFuture.channel().close();
        error(downstream, connectFuture.cause());
      }
    });
  }

  protected ReceivedResponse toReceivedResponse(FullHttpResponse msg) {
    return toReceivedResponse(msg, initBufferReleaseOnExecutionClose(msg.content(), execution));
  }

  protected ReceivedResponse toReceivedResponse(HttpResponse msg) {
    return toReceivedResponse(msg, byteBufAllocator.buffer(0, 0));
  }

  private ReceivedResponse toReceivedResponse(HttpResponse msg, ByteBuf responseBuffer) {
    final Headers headers = new NettyHeadersBackedHeaders(msg.headers());
    String contentType = headers.get(HttpHeaderConstants.CONTENT_TYPE.toString());
    final ByteBufBackedTypedData typedData = new ByteBufBackedTypedData(responseBuffer, DefaultMediaType.get(contentType));
    final Status status = new DefaultStatus(msg.status());
    return new DefaultReceivedResponse(status, headers, typedData);
  }

  protected abstract RequestAction<T> buildRedirectRequestAction(Action<? super RequestSpec> redirectRequestConfig, URI locationUrl, int redirectCount);

  protected abstract void addResponseHandlers(ChannelPipeline p, Downstream<? super T> downstream);

  protected void success(Downstream<? super T> downstream, T value) {
    if (fired.compareAndSet(false, true)) {
      downstream.success(value);
    }
  }

  protected void error(Downstream<?> downstream, Throwable error) {
    if (fired.compareAndSet(false, true)) {
      downstream.error(error);
    }
  }

  private static boolean isRedirect(int code) {
    return code == 301 || code == 302 || code == 303 || code == 307;
  }

  private static String getFullPath(URI uri) {
    StringBuilder sb = new StringBuilder(uri.getRawPath());
    String query = uri.getRawQuery();
    if (query != null) {
      sb.append("?").append(query);
    }

    return sb.toString();
  }

}
