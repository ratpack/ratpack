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

import com.google.common.net.HostAndPort;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.codec.PrematureChannelClosureException;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import ratpack.exec.Downstream;
import ratpack.exec.Execution;
import ratpack.exec.Upstream;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.http.Headers;
import ratpack.http.Status;
import ratpack.http.client.HttpClientReadTimeoutException;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.http.internal.*;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

abstract class RequestActionSupport<T> implements Upstream<T> {

  private static final Pattern ABSOLUTE_PATTERN = Pattern.compile("^https?://.*");
  private static final String SSL_HANDLER_NAME = "ssl";
  private static final String CLIENT_CODEC_HANDLER_NAME = "clientCodec";
  private static final String READ_TIMEOUT_HANDLER_NAME = "readTimeout";
  private static final String REDIRECT_HANDLER_NAME = "redirect";
  private static final String DECOMPRESS_HANDLER_NAME = "decompressor";

  protected final HttpClientInternal client;
  protected final RequestConfig requestConfig;
  protected final Execution execution;

  private final HttpChannelKey channelKey;
  private ChannelPool channelPool;
  private final int redirectCount;
  private final Action<? super RequestSpec> requestConfigurer;

  private boolean fired;
  private boolean disposed;

  RequestActionSupport(URI uri, HttpClientInternal client, int redirectCount, Execution execution, Action<? super RequestSpec> requestConfigurer) throws Exception {
    this.requestConfigurer = requestConfigurer;
    this.requestConfig = RequestConfig.of(uri, client, requestConfigurer);
    this.client = client;
    this.execution = execution;
    this.redirectCount = redirectCount;
    this.channelKey = new HttpChannelKey(requestConfig.uri, requestConfig.connectTimeout, execution);
    this.channelPool = client.getChannelPoolMap().get(channelKey);

    finalizeHeaders();
  }

  protected abstract void addResponseHandlers(ChannelPipeline p, Downstream<? super T> downstream);

  @Override
  public void connect(final Downstream<? super T> downstream) throws Exception {
    channelPool.acquire().addListener(acquireFuture -> {
      if (acquireFuture.isSuccess()) {
        Channel channel = (Channel) acquireFuture.getNow();
        if (channel.eventLoop().equals(execution.getEventLoop())) {
          send(downstream, channel);
        } else {
          channel.deregister().addListener(deregisterFuture ->
            execution.getEventLoop().register(channel).addListener(registerFuture -> {
              if (registerFuture.isSuccess()) {
                send(downstream, channel);
              } else {
                channel.close();
                channelPool.release(channel);
                connectFailure(downstream, registerFuture.cause());
              }
            })
          );
        }
      } else {
        connectFailure(downstream, acquireFuture.cause());
      }
    });
  }

  private void send(Downstream<? super T> downstream, Channel channel) throws Exception {
    channel.config().setAutoRead(true);

    FullHttpRequest request = new DefaultFullHttpRequest(
      HttpVersion.HTTP_1_1,
      requestConfig.method.getNettyMethod(),
      getFullPath(requestConfig.uri),
      requestConfig.body.touch(),
      requestConfig.headers.getNettyHeaders(),
      EmptyHttpHeaders.INSTANCE
    );

    addCommonResponseHandlers(channel.pipeline(), downstream);

    Future<?> channelFuture;
    if (channelKey.ssl) {
      channelFuture = channel.pipeline().get(SslHandler.class).handshakeFuture();
    } else {
      channelFuture = channel.newSucceededFuture();
    }

    channelFuture.addListener(firstFuture -> {
      if (firstFuture.isSuccess()) {
        channel.writeAndFlush(request).addListener(writeFuture -> {
          if (!writeFuture.isSuccess()) {
            error(downstream, writeFuture.cause());
          }
        });
      } else {
        error(downstream, firstFuture.cause());
      }
    });
  }

  private void connectFailure(Downstream<? super T> downstream, Throwable e) {
    ReferenceCountUtil.release(requestConfig.body);

    if (e instanceof ConnectTimeoutException) {
      StackTraceElement[] stackTrace = e.getStackTrace();
      e = new ConnectTimeoutException("Connect timeout (" + requestConfig.connectTimeout + ") connecting to " + requestConfig.uri);
      e.setStackTrace(stackTrace);
    }
    error(downstream, e);
  }

  private void finalizeHeaders() {
    if (requestConfig.headers.get(HttpHeaderConstants.HOST) == null) {
      HostAndPort hostAndPort = HostAndPort.fromParts(channelKey.host, channelKey.port);
      requestConfig.headers.set(HttpHeaderConstants.HOST, hostAndPort.toString());
    }
    if (client.getPoolSize() == 0) {
      requestConfig.headers.set(HttpHeaderConstants.CONNECTION, HttpHeaderValues.CLOSE);
    }
    int contentLength = requestConfig.body.readableBytes();
    if (contentLength > 0) {
      requestConfig.headers.set(HttpHeaderConstants.CONTENT_LENGTH, Integer.toString(contentLength));
    }
  }

  protected void forceDispose(ChannelPipeline channelPipeline) {
    dispose(channelPipeline, true);
  }

  protected void dispose(ChannelPipeline channelPipeline, HttpResponse response) {
    dispose(channelPipeline, !HttpUtil.isKeepAlive(response));
  }

  private void dispose(ChannelPipeline channelPipeline, boolean forceClose) {
    if (!disposed) {
      disposed = true;
      doDispose(channelPipeline, forceClose);
    }
  }

  protected void doDispose(ChannelPipeline channelPipeline, boolean forceClose) {
    channelPipeline.remove(CLIENT_CODEC_HANDLER_NAME);
    channelPipeline.remove(READ_TIMEOUT_HANDLER_NAME);
    channelPipeline.remove(REDIRECT_HANDLER_NAME);

    if (channelPipeline.get(DECOMPRESS_HANDLER_NAME) != null) {
      channelPipeline.remove(DECOMPRESS_HANDLER_NAME);
    }

    Channel channel = channelPipeline.channel();
    if (forceClose && channel.isOpen()) {
      channel.close();
    }

    channelPool.release(channel);
  }

  private void addCommonResponseHandlers(ChannelPipeline p, Downstream<? super T> downstream) throws Exception {
    if (channelKey.ssl && p.get(SSL_HANDLER_NAME) == null) {
      //this is added once because netty is not able to properly replace this handler on
      //pooled channels from request to request. Because a pool is unique to a uri,
      //doing this works, as subsequent requests would be passing in the same certs.
      p.addLast(SSL_HANDLER_NAME, createSslHandler());
    }

    p.addLast(CLIENT_CODEC_HANDLER_NAME, new HttpClientCodec(4096, 8192, requestConfig.responseMaxChunkSize, false));

    p.addLast(READ_TIMEOUT_HANDLER_NAME, new ReadTimeoutHandler(requestConfig.readTimeout.toNanos(), TimeUnit.NANOSECONDS));

    p.addLast(REDIRECT_HANDLER_NAME, new SimpleChannelInboundHandler<HttpObject>(false) {
      boolean redirected;
      HttpResponse response;

      @Override
      public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireExceptionCaught(new PrematureChannelClosureException("Server " + requestConfig.uri + " closed the connection prematurely"));
      }

      @Override
      protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpResponse) {
          this.response = (HttpResponse) msg;
          int maxRedirects = requestConfig.maxRedirects;
          int status = response.status().code();
          String locationValue = response.headers().getAsString(HttpHeaderConstants.LOCATION);

          Action<? super RequestSpec> redirectConfigurer = RequestActionSupport.this.requestConfigurer;
          if (isRedirect(status) && redirectCount < maxRedirects && locationValue != null) {
            final Function<? super ReceivedResponse, Action<? super RequestSpec>> onRedirect = requestConfig.onRedirect;
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
                  s.get();
                }
              };
              redirectRequestConfig = redirectConfigurer.append(redirectRequestConfig);

              URI locationUrl;
              if (ABSOLUTE_PATTERN.matcher(locationValue).matches()) {
                locationUrl = new URI(locationValue);
              } else {
                locationUrl = new URI(channelKey.ssl ? "https" : "http", null, channelKey.host, channelKey.port, locationValue, null, null);
              }

              onRedirect(locationUrl, redirectCount + 1, redirectRequestConfig).connect(downstream);

              redirected = true;
              dispose(ctx.pipeline(), response);
            }
          }
        }

        if (!redirected) {
          ctx.fireChannelRead(msg);
        }
      }
    });

    if (requestConfig.decompressResponse) {
      p.addLast(DECOMPRESS_HANDLER_NAME, new HttpContentDecompressor());
    }

    addResponseHandlers(p, downstream);
  }

  private SslHandler createSslHandler() throws NoSuchAlgorithmException, SSLException {
    SSLEngine sslEngine;
    if (requestConfig.sslContext != null) {
      sslEngine = createSslEngine(requestConfig.sslContext);
    } else {
      sslEngine = createSslEngine(SslContextBuilder.forClient().build());
    }
    sslEngine.setUseClientMode(true);
    SSLParameters sslParameters = sslEngine.getSSLParameters();
    sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
    sslEngine.setSSLParameters(sslParameters);
    return new SslHandler(sslEngine);
  }

  private SSLEngine createSslEngine(SslContext sslContext) {
    int port = requestConfig.uri.getPort();
    if (port == -1) {
      port = 443;
    }
    return sslContext.newEngine(client.getByteBufAllocator(), requestConfig.uri.getHost(), port);
  }

  protected abstract Upstream<T> onRedirect(URI locationUrl, int redirectCount, Action<? super RequestSpec> redirectRequestConfig) throws Exception;

  protected void success(Downstream<? super T> downstream, T value) {
    if (!fired) {
      fired = true;
      downstream.success(value);
    }
  }

  protected void error(Downstream<?> downstream, Throwable error) {
    if (!fired && !disposed) {
      fired = true;
      downstream.error(error);
    }
  }

  private ReceivedResponse toReceivedResponse(HttpResponse msg) {
    return toReceivedResponse(msg, Unpooled.EMPTY_BUFFER);
  }

  protected ReceivedResponse toReceivedResponse(HttpResponse msg, ByteBuf responseBuffer) {
    responseBuffer.touch();
    final Headers headers = new NettyHeadersBackedHeaders(msg.headers());
    String contentType = headers.get(HttpHeaderConstants.CONTENT_TYPE);
    final ByteBufBackedTypedData typedData = new ByteBufBackedTypedData(responseBuffer, DefaultMediaType.get(contentType));
    final Status status = new DefaultStatus(msg.status());
    return new DefaultReceivedResponse(status, headers, typedData);
  }

  private static boolean isRedirect(int code) {
    return code == 301 || code == 302 || code == 303 || code == 307;
  }

  protected Throwable decorateException(Throwable cause) {
    if (cause instanceof ReadTimeoutException) {
      cause = new HttpClientReadTimeoutException("Read timeout (" + requestConfig.readTimeout + ") waiting on HTTP server at " + requestConfig.uri);
    }
    return cause;
  }

  private static String getFullPath(URI uri) {
    String path = uri.getRawPath();
    String query = uri.getRawQuery();
    String fragment = uri.getRawFragment();

    if (query == null && fragment == null) {
      return path;
    } else {
      StringBuilder sb = new StringBuilder(path);
      if (query != null) {
        sb.append("?").append(query);
      }
      if (fragment != null) {
        sb.append("#").append(fragment);
      }
      return sb.toString();
    }
  }

}
