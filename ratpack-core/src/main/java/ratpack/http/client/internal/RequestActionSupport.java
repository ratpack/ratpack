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
import io.netty.channel.*;
import io.netty.channel.pool.ChannelPool;
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
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.http.internal.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static ratpack.util.Exceptions.uncheck;

abstract class RequestActionSupport<T> implements RequestAction<T> {

  private static final Pattern ABSOLUTE_PATTERN = Pattern.compile("^https?://.*");

  private final Action<? super RequestSpec> requestConfigurer;
  private final URI uri;
  protected final HttpClientInternal client;
  protected final ChannelPool channelPool;
  private final MutableHeaders headers;
  private final RequestSpecBacking requestSpecBacking;
  protected final HttpChannelKey channelKey;
  protected final RequestParams requestParams;
  protected final Execution execution;
  private final int redirectCount;

  private final AtomicBoolean fired = new AtomicBoolean();

  public RequestActionSupport(Action<? super RequestSpec> requestConfigurer, URI uri, HttpClientInternal client, Execution execution, int redirectCount) {
    this.requestConfigurer = requestConfigurer;
    this.uri = uri;
    this.client = client;
    this.requestParams = new RequestParams();
    this.headers = new NettyHeadersBackedMutableHeaders(new DefaultHttpHeaders());
    this.requestSpecBacking = new RequestSpecBacking(headers, uri, client.getByteBufAllocator(), requestParams);
    this.execution = execution;
    this.redirectCount = redirectCount;

    try {
      this.requestConfigurer.execute(requestSpecBacking.asSpec());
    } catch (Exception e) {
      throw uncheck(e);
    }

    this.channelKey = new HttpChannelKey(uri, requestParams.connectTimeout);
    this.channelPool = client.getChannelPoolMap().get(channelKey);
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
    channelPool.acquire().addListener(f1 -> {
      if (!f1.isSuccess()) {
        error(downstream, f1.cause());
      } else {
        Channel channel = (Channel) f1.getNow();

        addCommonResponseHandlers(channel.pipeline(), downstream);

        String fullPath = getFullPath(uri);
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.valueOf(requestSpecBacking.getMethod()), fullPath, requestSpecBacking.getBody());
        if (headers.get(HttpHeaderConstants.HOST) == null) {
          HostAndPort hostAndPort = HostAndPort.fromParts(channelKey.host, channelKey.port);
          headers.set(HttpHeaderConstants.HOST, hostAndPort.toString());
        }
        if (client.getPoolSize() > 0) {
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
    if (channelKey.ssl) {
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

    long readTimeout = requestParams.readTimeoutNanos > 0 ? requestParams.readTimeoutNanos : client.getReadTimeout().toNanos();
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
              channelPool.release(ctx.channel());
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
    return toReceivedResponse(msg, client.getByteBufAllocator().buffer(0, 0));
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

}
