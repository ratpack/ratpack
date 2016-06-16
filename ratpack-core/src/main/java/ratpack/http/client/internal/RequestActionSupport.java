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
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.handler.codec.PrematureChannelClosureException;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.Downstream;
import ratpack.exec.Execution;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.http.Headers;
import ratpack.http.MutableHeaders;
import ratpack.http.Status;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.http.internal.ByteBufBackedTypedData;
import ratpack.http.internal.DefaultMediaType;
import ratpack.http.internal.DefaultStatus;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.http.internal.NettyHeadersBackedHeaders;
import ratpack.http.internal.NettyHeadersBackedMutableHeaders;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static ratpack.util.Exceptions.uncheck;

public abstract class RequestActionSupport<T> implements RequestAction<T> {
  public static final Logger LOGGER = LoggerFactory.getLogger(RequestActionSupport.class);
  private static final Pattern ABSOLUTE_PATTERN = Pattern.compile("^https?://.*");

  private final Action<? super RequestSpec> requestConfigurer;
  protected final ChannelPoolMap<URI, ChannelPool> channelPoolMap;
  protected final PooledHttpConfig config;
  private final MutableHeaders headers;
  private final RequestSpecBacking requestSpecBacking;
  protected final ByteBufAllocator byteBufAllocator;
  protected final URI uri;
  protected final URI baseURI;
  protected final RequestParams requestParams;
  private final boolean finalUseSsl;
  private final String host;
  private final int port;
  private final int redirectCount;
  private final AtomicBoolean fired = new AtomicBoolean();
  protected final Execution execution;

  public RequestActionSupport(Action<? super RequestSpec> requestConfigurer, ChannelPoolMap<URI, ChannelPool> channelPoolMap, PooledHttpConfig config, URI uri, ByteBufAllocator byteBufAllocator, Execution execution, int redirectCount) {
    this.requestConfigurer = requestConfigurer;
    this.channelPoolMap = channelPoolMap;
    this.config = config;
    this.byteBufAllocator = byteBufAllocator;
    this.uri = uri;
    this.baseURI = constructBaseURI(uri);
    this.requestParams = new RequestParams();
    this.headers = new NettyHeadersBackedMutableHeaders(new DefaultHttpHeaders());
    this.requestSpecBacking = new RequestSpecBacking(headers, uri, byteBufAllocator, requestParams);
    this.execution = execution;
    this.redirectCount = redirectCount;

    try {
      this.requestConfigurer.execute(requestSpecBacking.asSpec());
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

  private static String getFullPath(URI uri) {
    StringBuilder sb = new StringBuilder(uri.getRawPath());
    String query = uri.getRawQuery();
    if (query != null) {
      sb.append("?").append(query);
    }

    return sb.toString();
  }

  protected abstract void addResponseHandlers(ChannelPipeline p, Downstream<? super T> downstream);

  @Override
  public void connect(final Downstream<? super T> downstream) throws Exception {
    Future<Channel> connectFuture = this.channelPoolMap.get(baseURI).acquire();
    connectFuture.addListener(f1 -> {
      if (!connectFuture.isSuccess()) {
        error(downstream, connectFuture.cause());
      } else {
        Channel channel = connectFuture.getNow();
        addCommonResponseHandlers(channel.pipeline(), downstream);

        String fullPath = getFullPath(uri);
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.valueOf(requestSpecBacking.getMethod()), fullPath, requestSpecBacking.getBody());
        if (headers.get(HttpHeaderConstants.HOST) == null) {
          HostAndPort hostAndPort = HostAndPort.fromParts(host, port);
          headers.set(HttpHeaderConstants.HOST, hostAndPort.toString());
        }
        if (config.isPooled()) {
          headers.set(HttpHeaderConstants.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        } else {
          headers.set(HttpHeaderConstants.CONNECTION, HttpHeaderValues.CLOSE);
        }
        int contentLength = request.content().readableBytes();
        if (contentLength > 0) {
          headers.set(HttpHeaderConstants.CONTENT_LENGTH, Integer.toString(contentLength));
        }

        HttpHeaders requestHeaders = request.headers();
        requestHeaders.set(headers.getNettyHeaders());

        ChannelFuture writeFuture = channel.writeAndFlush(request);
        writeFuture.addListener(f2 -> {
          if (!writeFuture.isSuccess()) {
            error(downstream, writeFuture.cause());
          }
        });
      }
    });
  }

  protected void addCommonResponseHandlers(ChannelPipeline p, Downstream<? super T> downstream) throws Exception {

    if (finalUseSsl) {
      SSLEngine sslEngine;
      if (requestSpecBacking.getSslContext() != null) {
        sslEngine = requestSpecBacking.getSslContext().createSSLEngine();
      } else {
        sslEngine = SSLContext.getDefault().createSSLEngine();
      }
      sslEngine.setUseClientMode(true);
      //this is added once because netty is not able to properly replace this handler on
      //pooled channels from request to request. Because a pool is unique to a uri,
      //doing this works, as subsequent requests would be passing in the same certs.
      addHandlerOnce(p, "ssl", new SslHandler(sslEngine));
    }

    //this is added once because it is the same across all requests.
    addHandlerOnce(p, "clientCodec", new HttpClientCodec());

    long readTimeout = requestParams.readTimeoutNanos > 0 ? requestParams.readTimeoutNanos : config.getReadTimeoutNanos();
    addHandler(p, "readTimeout", new ReadTimeoutHandler(readTimeout, TimeUnit.NANOSECONDS));

    ChannelHandler redirectHandler = new SimpleChannelInboundHandler<HttpObject>(false) {
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
          if (isRedirect(status) && redirectCount < maxRedirects && locationValue != null) {
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

              buildRedirectRequestAction(redirectRequestConfig, locationUrl, redirectCount + 1).connect(downstream);
              redirected = true;
              channelPoolMap.get(baseURI).release(ctx.channel());
            }
          }
        }

        if (!redirected) {
          ctx.fireChannelRead(msg);
        }
      }
    };
    addHandler(p, "redirectHandler", redirectHandler);

    if (requestSpecBacking.isDecompressResponse()) {
      addHandler(p, "httpContentDecompressor", new HttpContentDecompressor());
    } else {
      removeHandler(p, "httpContentDecompressor");
    }

    addResponseHandlers(p, downstream);
  }

  protected abstract RequestAction<T> buildRedirectRequestAction(Action<? super RequestSpec> redirectRequestConfig, URI locationUrl, int redirectCount);

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

  protected ReceivedResponse toReceivedResponse(FullHttpResponse msg) {
    return toReceivedResponse(msg, initBufferReleaseOnExecutionClose(msg.content(), this.execution));
  }

  protected ReceivedResponse toReceivedResponse(HttpResponse msg) {
    return toReceivedResponse(msg, byteBufAllocator.buffer(0, 0));
  }

  protected void addHandler(ChannelPipeline p, String name, ChannelHandler channelHandler) {
    if (p.get(name) == null) {
      p.addLast(name, channelHandler);
    } else {
      p.replace(name, name, channelHandler);
    }
  }

  protected void addHandlerOnce(ChannelPipeline p, String name, ChannelHandler channelHandler) {
    if (p.get(name) == null) {
      p.addLast(name, channelHandler);
    }
  }

  protected void removeHandler(ChannelPipeline p, String name) {
    if (p.get(name) != null) {
      p.remove(name);
    }
  }

  private ReceivedResponse toReceivedResponse(HttpResponse msg, ByteBuf responseBuffer) {
    final Headers headers = new NettyHeadersBackedHeaders(msg.headers());
    String contentType = headers.get(HttpHeaderConstants.CONTENT_TYPE.toString());
    final ByteBufBackedTypedData typedData = new ByteBufBackedTypedData(responseBuffer, DefaultMediaType.get(contentType));
    final Status status = new DefaultStatus(msg.status());
    return new DefaultReceivedResponse(status, headers, typedData);
  }

  private ByteBuf initBufferReleaseOnExecutionClose(final ByteBuf responseBuffer, Execution execution) {
    execution.onComplete(responseBuffer::release);
    return responseBuffer;
  }

  private static boolean isRedirect(int code) {
    return code == 301 || code == 302 || code == 303 || code == 307;
  }

  private URI constructBaseURI(URI uri) {
    try {
      return new URI(uri.getScheme(),
        null,
        uri.getHost(),
        uri.getPort(),
        null,
        null,
        null);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Unable to create valid baseURI from " + uri);
    }
  }

}
