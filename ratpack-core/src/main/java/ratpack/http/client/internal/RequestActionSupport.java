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
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import ratpack.exec.Downstream;
import ratpack.exec.Execution;
import ratpack.exec.Upstream;
import ratpack.func.Action;
import ratpack.func.Factory;
import ratpack.func.Function;
import ratpack.http.Headers;
import ratpack.http.Status;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.http.internal.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static ratpack.util.Exceptions.uncheck;

abstract class RequestActionSupport<T> implements Upstream<T> {

  private static final Pattern ABSOLUTE_PATTERN = Pattern.compile("^https?://.*");

  protected final HttpClientInternal client;
  protected final ChannelPool channelPool;
  protected final RequestConfig requestConfig;
  protected final HttpChannelKey channelKey;
  protected final Execution execution;

  private final int redirectCount;
  private final Action<? super RequestSpec> requestConfigurer;

  private boolean fired;

  RequestActionSupport(URI uri, HttpClientInternal client, int redirectCount, Execution execution, Action<? super RequestSpec> requestConfigurer) {
    this.requestConfigurer = requestConfigurer;
    this.requestConfig = uncheck(() -> RequestConfig.of(uri, client, requestConfigurer));
    this.client = client;
    this.execution = execution;
    this.redirectCount = redirectCount;
    this.channelKey = new HttpChannelKey(requestConfig.uri, requestConfig.connectTimeout);
    this.channelPool = client.getChannelPoolMap().get(channelKey);
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

        String fullPath = getFullPath(requestConfig.uri);
        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, requestConfig.method.getNettyMethod(), fullPath, requestConfig.body);
        if (requestConfig.headers.get(HttpHeaderConstants.HOST) == null) {
          HostAndPort hostAndPort = HostAndPort.fromParts(channelKey.host, channelKey.port);
          requestConfig.headers.set(HttpHeaderConstants.HOST, hostAndPort.toString());
        }
        if (client.getPoolSize() > 0) {
          requestConfig.headers.set(HttpHeaderConstants.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        } else {
          requestConfig.headers.set(HttpHeaderConstants.CONNECTION, HttpHeaderValues.CLOSE);
        }
        int contentLength = request.content().readableBytes();
        if (contentLength > 0) {
          requestConfig.headers.set(HttpHeaderConstants.CONTENT_LENGTH, Integer.toString(contentLength));
        }

        HttpHeaders requestHeaders = request.headers();
        requestHeaders.set(requestConfig.headers.getNettyHeaders());

        ChannelFuture writeFuture = channel.writeAndFlush(request);
        writeFuture.addListener(f2 -> {
          if (!writeFuture.isSuccess()) {
            error(downstream, writeFuture.cause());
          }
        });
      }
    });
  }

  private void addCommonResponseHandlers(ChannelPipeline p, Downstream<? super T> downstream) throws Exception {
    if (channelKey.ssl) {
      //this is added once because netty is not able to properly replace this handler on
      //pooled channels from request to request. Because a pool is unique to a uri,
      //doing this works, as subsequent requests would be passing in the same certs.
      addHandlerOnce(p, "ssl", () -> {
        SSLEngine sslEngine;
        if (requestConfig.sslContext != null) {
          sslEngine = requestConfig.sslContext.createSSLEngine();
        } else {
          sslEngine = SSLContext.getDefault().createSSLEngine();
        }
        sslEngine.setUseClientMode(true);
        return new SslHandler(sslEngine);
      });
    }

    //this is added once because it is the same across all requests.
    addHandlerOnce(p, "clientCodec", HttpClientCodec::new);

    addHandler(p, "readTimeout", new ReadTimeoutHandler(requestConfig.readTimeout.toNanos(), TimeUnit.NANOSECONDS));

    ChannelHandler redirectHandler = new SimpleChannelInboundHandler<HttpObject>(false) {
      boolean readComplete;
      boolean redirected;

      @Override
      public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (!readComplete) {
          error(downstream, new PrematureChannelClosureException("Server " + requestConfig.uri + " closed the connection prematurely"));
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
              redirectRequestConfig = redirectRequestConfig.append(redirectConfigurer);

              URI locationUrl;
              if (ABSOLUTE_PATTERN.matcher(locationValue).matches()) {
                locationUrl = new URI(locationValue);
              } else {
                locationUrl = new URI(channelKey.ssl ? "https" : "http", null, channelKey.host, channelKey.port, locationValue, null, null);
              }

              onRedirect(locationUrl, redirectCount + 1, redirectRequestConfig).connect(downstream);

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

    if (requestConfig.decompressResponse) {
      addHandler(p, "httpContentDecompressor", new HttpContentDecompressor());
    } else {
      removeHandler(p, "httpContentDecompressor");
    }

    addResponseHandlers(p, downstream);
  }

  protected abstract Upstream<T> onRedirect(URI locationUrl, int redirectCount, Action<? super RequestSpec> redirectRequestConfig);

  protected void success(Downstream<? super T> downstream, T value) {
    if (!fired) {
      fired = true;
      downstream.success(value);
    }
  }

  protected void error(Downstream<?> downstream, Throwable error) {
    if (!fired) {
      fired = true;
      downstream.error(error);
    }
  }

  ReceivedResponse toReceivedResponse(FullHttpResponse msg) {
    return toReceivedResponse(msg, initBufferReleaseOnExecutionClose(msg.content(), execution));
  }

  private ReceivedResponse toReceivedResponse(HttpResponse msg) {
    return toReceivedResponse(msg, Unpooled.EMPTY_BUFFER);
  }

  void addHandler(ChannelPipeline p, String name, ChannelHandler channelHandler) {
    if (p.get(name) == null) {
      p.addLast(name, channelHandler);
    } else {
      p.replace(name, name, channelHandler);
    }
  }

  private void addHandlerOnce(ChannelPipeline p, String name, Factory<? extends ChannelHandler> channelHandler) throws Exception {
    if (p.get(name) == null) {
      p.addLast(name, channelHandler.create());
    }
  }

  private void removeHandler(ChannelPipeline p, String name) {
    if (p.get(name) != null) {
      p.remove(name);
    }
  }

  private ReceivedResponse toReceivedResponse(HttpResponse msg, ByteBuf responseBuffer) {
    final Headers headers = new NettyHeadersBackedHeaders(msg.headers());
    String contentType = headers.get(HttpHeaderConstants.CONTENT_TYPE);
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
