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

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import ratpack.exec.Execution;
import ratpack.exec.Fulfiller;
import ratpack.func.Action;
import ratpack.http.Headers;
import ratpack.http.MutableHeaders;
import ratpack.http.Status;
import ratpack.http.client.RequestSpec;
import ratpack.http.internal.DefaultStatus;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.http.internal.NettyHeadersBackedHeaders;
import ratpack.http.internal.NettyHeadersBackedMutableHeaders;
import ratpack.util.internal.ChannelImplDetector;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static ratpack.util.ExceptionUtils.uncheck;

abstract class RequestActionSupport<T> implements RequestAction<T> {

  private static final Pattern ABSOLUTE_PATTERN = Pattern.compile("^https?://.*");

  private final Action<? super RequestSpec> requestConfigurer;
  private final boolean finalUseSsl;
  private final String host;
  private final int port;
  private final MutableHeaders headers;
  private final RequestSpecBacking requestSpecBacking;
  private final URI uri;
  private final RequestParams requestParams;
  private final AtomicBoolean fired = new AtomicBoolean();

  protected final Execution execution;
  protected final ByteBufAllocator byteBufAllocator;

  public RequestActionSupport(Action<? super RequestSpec> requestConfigurer, URI uri, Execution execution, ByteBufAllocator byteBufAllocator) {
    this.execution = execution;
    this.requestConfigurer = requestConfigurer;
    this.byteBufAllocator = byteBufAllocator;
    this.uri = uri;
    this.requestParams = new RequestParams();
    this.headers = new NettyHeadersBackedMutableHeaders(new DefaultHttpHeaders());
    this.requestSpecBacking = new RequestSpecBacking(headers, uri, byteBufAllocator, requestParams);

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

  public void execute(final Fulfiller<? super T> fulfiller) throws Exception {
    final AtomicBoolean redirecting = new AtomicBoolean();

    final Bootstrap b = new Bootstrap();
    b.group(this.execution.getEventLoop())
      .channel(ChannelImplDetector.getSocketChannelImpl())
      .handler(new ChannelInitializer<SocketChannel>() {
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
          ChannelPipeline p = ch.pipeline();

          if (finalUseSsl) {
            SSLEngine engine = SSLContext.getDefault().createSSLEngine();
            engine.setUseClientMode(true);
            p.addLast("ssl", new SslHandler(engine));
          }

          p.addLast("codec", new HttpClientCodec());
          p.addLast("readTimeout", new ReadTimeoutHandler(requestParams.readTimeoutNanos, TimeUnit.NANOSECONDS));

          p.addLast("redirectHandler", new SimpleChannelInboundHandler<HttpObject>(false) {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
              if (msg instanceof HttpResponse) {
                final HttpResponse response = (HttpResponse) msg;
                final Headers headers = new NettyHeadersBackedHeaders(response.headers());
                final Status status = new DefaultStatus(response.status());
                int maxRedirects = requestSpecBacking.getMaxRedirects();
                String locationValue = headers.get("Location");

                //Check for redirect and location header if it is follow redirect if we have request forwarding left
                if (shouldRedirect(status) && maxRedirects > 0 && locationValue != null) {
                  redirecting.compareAndSet(false, true);

                  Action<? super RequestSpec> redirectRequestConfig = Action.join(requestConfigurer, s -> {
                    if (status.getCode() == 301 || status.getCode() == 302) {
                      s.method("GET");
                    }


                    s.redirects(maxRedirects - 1);
                  });

                  URI locationUrl;
                  if (ABSOLUTE_PATTERN.matcher(locationValue).matches()) {
                    locationUrl = new URI(locationValue);
                  } else {
                    locationUrl = new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), locationValue, null, null);
                  }

                  buildRedirectRequestAction(redirectRequestConfig, locationUrl).execute(fulfiller);
                } else {
                  p.remove(this);
                }
              }

              if (!redirecting.get()) {
                ctx.fireChannelRead(msg);
              }
            }
          });

          addResponseHandlers(p, fulfiller);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
          ctx.close();
          error(fulfiller, cause);
        }
      });

    ChannelFuture connectFuture = b.connect(host, port);
    connectFuture.addListener(f1 -> {
      if (connectFuture.isSuccess()) {
        String fullPath = getFullPath(uri);
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.valueOf(requestSpecBacking.getMethod()), fullPath, requestSpecBacking.getBody());
        if (headers.get(HttpHeaderConstants.HOST) == null) {
          headers.set(HttpHeaderConstants.HOST, host);
        }
        headers.set(HttpHeaderConstants.CONNECTION, HttpHeaderValues.CLOSE);
        int contentLength = request.content().readableBytes();
        if (contentLength > 0) {
          headers.set(HttpHeaderConstants.CONTENT_LENGTH, Integer.toString(contentLength, 10));
        }

        HttpHeaders requestHeaders = request.headers();

        for (String name : headers.getNames()) {
          requestHeaders.set(name, headers.getAll(name));
        }

        ChannelFuture writeFuture = connectFuture.channel().writeAndFlush(request);
        writeFuture.addListener(f2 -> {
          if (!writeFuture.isSuccess()) {
            writeFuture.channel().close();
            error(fulfiller, writeFuture.cause());
          }
        });
      } else {
        connectFuture.channel().close();
        error(fulfiller, connectFuture.cause());
      }
    });
  }

  protected abstract RequestAction<T> buildRedirectRequestAction(Action<? super RequestSpec> redirectRequestConfig, URI locationUrl);

  protected abstract void addResponseHandlers(ChannelPipeline p, Fulfiller<? super T> fulfiller);

  protected void success(Fulfiller<? super T> fulfiller, T value) {
    if (fired.compareAndSet(false, true)) {
      fulfiller.success(value);
    }
  }

  protected void error(Fulfiller<?> fulfiller, Throwable error) {
    if (fired.compareAndSet(false, true)) {
      fulfiller.error(error);
    }
  }

  private static boolean shouldRedirect(Status status) {
    int code = status.getCode();
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
