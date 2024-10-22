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
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import ratpack.exec.Downstream;
import ratpack.exec.Execution;
import ratpack.exec.Upstream;
import ratpack.exec.internal.DefaultExecution;
import ratpack.func.Action;
import ratpack.http.Headers;
import ratpack.http.Status;
import ratpack.http.client.HttpClientReadTimeoutException;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.http.internal.*;

import javax.annotation.Nullable;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

abstract class RequestActionSupport<T> implements Upstream<T> {

  private static final String SSL_HANDLER_NAME = "ssl";
  private static final String CLIENT_CODEC_HANDLER_NAME = "clientCodec";
  private static final String READ_TIMEOUT_HANDLER_NAME = "readTimeout";
  private static final String REDIRECT_HANDLER_NAME = "redirect";
  private static final String REDIRECT_AGGREGATOR_HANDLER_NAME = "redirect-aggregator";
  private static final String REDIRECT_RESPONDER_HANDLER_NAME = "redirect-responder";
  private static final String DECOMPRESS_HANDLER_NAME = "decompressor";
  private static final String WRITABILITY_HANDLER_NAME = "writability";

  protected final HttpClientInternal client;
  protected final RequestConfig requestConfig;
  protected final Execution execution;

  private final HttpChannelKey channelKey;
  private final ChannelPool channelPool;
  private final int redirectCount;
  private final Action<? super RequestSpec> requestConfigurer;

  private boolean disposed;
  private boolean expectContinue;
  private boolean receivedContinue;
  private boolean streamingBody;
  private boolean aggregatingRedirect;

  private static final Runnable NOOP_RUNNABLE = () -> {
  };
  private Runnable onWritabilityChanged = NOOP_RUNNABLE;

  RequestActionSupport(URI uri, HttpClientInternal client, int redirectCount, boolean expectContinue, Execution execution, Action<? super RequestSpec> requestConfigurer) throws Exception {
    this.requestConfigurer = requestConfigurer;
    this.requestConfig = RequestConfig.of(uri, client, requestConfigurer);
    this.client = client;
    this.execution = execution;
    this.redirectCount = redirectCount;
    this.expectContinue = expectContinue;
    this.channelKey = new HttpChannelKey(requestConfig.uri, proxy(client), requestConfig.connectTimeout, execution);
    this.channelPool = client.getChannelPoolMap().get(channelKey);

    finalizeHeaders();
  }

  private ProxyInternal proxy(HttpClientInternal client) {
    return requestConfig.proxy == null ? client.getProxyInternal() : requestConfig.proxy;
  }

  protected abstract void addResponseHandlers(ChannelPipeline p, Downstream<? super T> downstream);

  @Override
  public void connect(final Downstream<? super T> downstream) {
    channelPool.acquire().addListener(acquireFuture -> {
      if (acquireFuture.isSuccess()) {
        Channel channel = (Channel) acquireFuture.getNow();
        channel.config().setAutoClose(false);
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
    expectContinue = requestConfig.headers.getNettyHeaders().contains(HttpHeaderNames.EXPECT, HttpHeaderValues.CONTINUE, true);
    boolean streamedBody = !requestConfig.content.isBuffer();

    String requestUri = getFullPath(requestConfig.uri);
    HttpMessage request;
    if (requestConfig.content.getContentLength() == 0) {
      requestConfig.content.discard();
      request = new DefaultFullHttpRequest(
        HttpVersion.HTTP_1_1,
        requestConfig.method.getNettyMethod(),
        requestUri,
        Unpooled.EMPTY_BUFFER,
        requestConfig.headers.getNettyHeaders(),
        EmptyHttpHeaders.INSTANCE
      );
    } else {
      if (!expectContinue && !streamedBody) {
        request = new DefaultFullHttpRequest(
          HttpVersion.HTTP_1_1,
          requestConfig.method.getNettyMethod(),
          requestUri,
          requestConfig.content.buffer(),
          requestConfig.headers.getNettyHeaders(),
          EmptyHttpHeaders.INSTANCE
        );
      } else {
        request = new DefaultHttpRequest(
          HttpVersion.HTTP_1_1,
          requestConfig.method.getNettyMethod(),
          requestUri,
          requestConfig.headers.getNettyHeaders()
        );
      }
    }

    HttpUtil.setTransferEncodingChunked(request, streamedBody && requestConfig.content.getContentLength() < 0);

    addCommonResponseHandlers(channel.pipeline(), downstream);

    Future<?> channelFuture;
    if (channelKey.ssl) {
      channelFuture = channel.pipeline().get(SslHandler.class).handshakeFuture();
    } else {
      channelFuture = channel.newSucceededFuture();
    }

    channelFuture.addListener(firstFuture -> {
      if (firstFuture.isSuccess()) {
        channel.writeAndFlush(request)
          .addListener(writeFuture -> {
            if (writeFuture.isSuccess()) {
              if (!expectContinue && streamedBody) {
                sendRequestBodyStream(downstream, channel, requestConfig.content.publisher());
              }
            } else {
              forceDispose(channel.pipeline())
                .addListener(disposeFuture -> {
                  if (!disposeFuture.isSuccess()) {
                    writeFuture.cause().addSuppressed(disposeFuture.cause());
                  }
                  downstream.error(writeFuture.cause());
                });
            }
          });
      }
    });
  }

  private void sendRequestBody(Downstream<? super T> downstream, Channel channel) {
    RequestConfig.Content content = requestConfig.content;
    if (content.isBuffer()) {
      channel.writeAndFlush(new DefaultLastHttpContent(content.buffer()))
        .addListener(future -> {
          if (!future.isSuccess() && channel.isOpen()) {
            forceDispose(channel.pipeline());
            downstream.error(future.cause());
          }
        });
    } else {
      sendRequestBodyStream(downstream, channel, content.publisher());
    }
  }

  private void sendRequestBodyStream(Downstream<? super T> downstream, Channel channel, Publisher<? extends ByteBuf> publisher) {
    streamingBody = true;
    ((DefaultExecution) execution).delimit(downstream::error, continuation -> {
        continuation.resume(() -> {
            publisher.subscribe(new Subscriber<ByteBuf>() {
              private Subscription subscription;

              private final AtomicBoolean done = new AtomicBoolean();
              private long pending = requestConfig.content.getContentLength();

              private final GenericFutureListener<Future<? super Void>> cancelOnCloseListener =
                c -> cancel();

              private void cancel() {
                subscription.cancel();
                reset();
              }

              @Override
              public void onSubscribe(Subscription subscription) {
                if (subscription == null) {
                  throw new NullPointerException("'subscription' is null");
                }
                if (this.subscription != null) {
                  subscription.cancel();
                  return;
                }

                this.subscription = subscription;

                if (channel.isOpen()) {
                  channel.closeFuture().addListener(cancelOnCloseListener);
                  if (channel.isWritable()) {
                    this.subscription.request(1);
                  }
                  onWritabilityChanged = () -> {
                    if (channel.isWritable() && !done.get()) {
                      this.subscription.request(1);
                    }
                  };
                } else {
                  cancel();
                }
              }

              @Override
              public void onNext(ByteBuf o) {
                o.touch();
                if (!channel.isOpen()) {
                  o.release();
                  cancel();
                  return;
                }

                if (pending == 0) {
                  o.release();
                  subscription.request(1);
                  return;
                }

                int chunkSize = o.readableBytes();
                if (pending > 0) {
                  if (chunkSize > pending) {
                    chunkSize = (int) pending;
                    o = o.slice(0, chunkSize);
                  }
                  pending = pending - chunkSize;
                }

                channel.write(new DefaultHttpContent(o));
                if (channel.isWritable()) {
                  subscription.request(1);
                } else {
                  channel.flush();
                }
              }

              @Override
              public void onError(Throwable t) {
                if (t == null) {
                  throw new NullPointerException("error is null");
                }
                reset();
                forceDispose(channel.pipeline())
                  .addListener(future -> {
                    if (!future.isSuccess()) {
                      t.addSuppressed(future.cause());
                    }
                    downstream.error(t);
                  });
              }

              @Override
              public void onComplete() {
                if (done.compareAndSet(false, true)) {
                  if (pending > 0) {
                    reset();
                    channel.flush();
                    forceDispose(channel.pipeline())
                      .addListener(future -> {
                        Throwable t = new IllegalStateException("Publisher completed before sending advertised number of bytes");
                        if (!future.isSuccess()) {
                          t.addSuppressed(future.cause());
                        }
                        downstream.error(t);
                      });
                  } else {
                    reset();
                    channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
                  }
                }
              }

              private void reset() {
                if (done.compareAndSet(false, true)) {
                  onWritabilityChanged = NOOP_RUNNABLE;
                  channel.closeFuture().removeListener(cancelOnCloseListener);
                  streamingBody = false;
                }
              }
            });
          }
        );
      }
    );
  }

  private void connectFailure(Downstream<? super T> downstream, Throwable e) {
    requestConfig.content.discard();

    if (e instanceof ConnectTimeoutException) {
      StackTraceElement[] stackTrace = e.getStackTrace();
      e = new ConnectTimeoutException("Connect timeout (" + requestConfig.connectTimeout + ") connecting to " + requestConfig.uri);
      e.setStackTrace(stackTrace);
    }

    downstream.error(e);
  }

  private void finalizeHeaders() {
    if (requestConfig.headers.get(HttpHeaderConstants.HOST) == null) {
      HostAndPort hostAndPort = HostAndPort.fromParts(channelKey.host, channelKey.port);
      requestConfig.headers.set(HttpHeaderConstants.HOST, hostAndPort.toString());
    }
    if (client.getPoolSize() == 0) {
      requestConfig.headers.set(HttpHeaderConstants.CONNECTION, HttpHeaderValues.CLOSE);
    }
    long contentLength = requestConfig.content.getContentLength();
    if (contentLength > 0) {
      requestConfig.headers.set(HttpHeaderConstants.CONTENT_LENGTH, Long.toString(contentLength));
    }
  }

  protected Future<Void> forceDispose(ChannelPipeline channelPipeline) {
    return dispose(channelPipeline, true);
  }

  protected Future<Void> dispose(ChannelPipeline channelPipeline, HttpResponse response) {
    return dispose(channelPipeline, !HttpUtil.isKeepAlive(response) || streamingBody);
  }

  private Future<Void> dispose(ChannelPipeline channelPipeline, boolean forceClose) {
    if (disposed) {
      return execution.getEventLoop().newSucceededFuture(null);
    } else {
      disposed = true;
      return doDispose(channelPipeline, forceClose);
    }
  }

  protected Future<Void> doDispose(ChannelPipeline channelPipeline, boolean forceClose) {
    channelPipeline.remove(CLIENT_CODEC_HANDLER_NAME);
    channelPipeline.remove(READ_TIMEOUT_HANDLER_NAME);
    if (aggregatingRedirect) {
      channelPipeline.remove(REDIRECT_AGGREGATOR_HANDLER_NAME);
      channelPipeline.remove(REDIRECT_RESPONDER_HANDLER_NAME);
    } else {
      channelPipeline.remove(REDIRECT_HANDLER_NAME);
    }
    channelPipeline.remove(WRITABILITY_HANDLER_NAME);

    if (channelPipeline.get(DECOMPRESS_HANDLER_NAME) != null) {
      channelPipeline.remove(DECOMPRESS_HANDLER_NAME);
    }

    Channel channel = channelPipeline.channel();
    if (forceClose && channel.isOpen()) {
      channel.close();
    }

    channel.config().setAutoClose(true);

    return channelPool.release(channel);
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
      HttpResponse response;

      @Override
      public void channelInactive(ChannelHandlerContext ctx) {
        ctx.fireExceptionCaught(new PrematureChannelClosureException("Server " + requestConfig.uri + " closed the connection prematurely"));
      }

      @Override
      protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        // received the end frame of the 100 Continue, send the body, then process the resulting response
        if (msg instanceof LastHttpContent && expectContinue && receivedContinue) {
          expectContinue = false;
          receivedContinue = false;
          sendRequestBody(downstream, ctx.channel());
          return;
        }

        if (msg instanceof HttpResponse) {
          this.response = (HttpResponse) msg;
          int status = response.status().code();

          if (expectContinue) {
            // need to wait for a 100 Continue to come in
            if (status == HttpResponseStatus.CONTINUE.code()) {
              // received the continue, now wait for the end frame before sending the body
              receivedContinue = true;
              return;
            } else if (!isRedirect(status)) {
              // Received a response other than 100 Continue and not a redirect, so clear that we expect a 100
              // and process this as the normal response without sending the body.
              expectContinue = false;
            }
          }

          String locationValue = response.headers().getAsString(HttpHeaderConstants.LOCATION);
          if (isRedirect(status) && redirectCount < requestConfig.maxRedirects && locationValue != null) {
            Upstream<T> redirector = maybeCreateRedirector(requestConfigurer, status, locationValue);
            if (redirector != null) {
              if (msg instanceof LastHttpContent) {
                doRedirect(ctx, redirector);
                return;
              } else {
                aggregatingRedirect = true;
                ctx.pipeline().addAfter(REDIRECT_HANDLER_NAME, REDIRECT_AGGREGATOR_HANDLER_NAME, new HttpObjectAggregator(requestConfig.maxContentLength));
                ctx.pipeline().addAfter(REDIRECT_AGGREGATOR_HANDLER_NAME, REDIRECT_RESPONDER_HANDLER_NAME, new SimpleChannelInboundHandler<FullHttpResponse>() {
                  @Override
                  protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) {
                    doRedirect(ctx, redirector);
                  }
                });
                ctx.pipeline().remove(REDIRECT_HANDLER_NAME);
                ctx.channel().config().setAutoRead(true);
              }
            }
          }
        }

        ctx.fireChannelRead(msg);
      }

      private void doRedirect(ChannelHandlerContext ctx, Upstream<T> redirector) {
        dispose(ctx.pipeline(), response)
          .addListener(future -> {
            if (future.isSuccess()) {
              redirector.connect(downstream);
            } else {
              downstream.error(future.cause());
            }
          });
      }

      @Nullable
      private Upstream<T> maybeCreateRedirector(Action<? super RequestSpec> redirectConfigurer, int status, String locationValue) throws Exception {
        if (requestConfig.onRedirect != null) {
          Action<? super RequestSpec> onRedirectResult = ((DefaultExecution) execution)
            .runSync(() -> requestConfig.onRedirect.apply(toReceivedResponse(response)));

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
          Action<? super RequestSpec> finalRedirectRequestConfig = redirectConfigurer.append(redirectRequestConfig);

          Action<? super RequestSpec> executionBoundRedirectRequestConfig = request ->
            ((DefaultExecution) execution).runSync(() -> {
              finalRedirectRequestConfig.execute(request);
              return null;
            });

          return onRedirect(
            absolutizeRedirect(requestConfig.uri, locationValue),
            redirectCount + 1,
            expectContinue,
            executionBoundRedirectRequestConfig);
        } else {
          return null;
        }
      }
    });

    if (requestConfig.decompressResponse) {
      p.addLast(DECOMPRESS_HANDLER_NAME, new HttpContentDecompressor());
    }

    p.addLast(WRITABILITY_HANDLER_NAME, new ChannelInboundHandlerAdapter() {
      @Override
      public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        onWritabilityChanged.run();
        super.channelWritabilityChanged(ctx);
      }
    });

    addResponseHandlers(p, downstream);
  }

  private SslHandler createSslHandler() throws SSLException {
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

  protected abstract Upstream<T> onRedirect(URI locationUrl, int redirectCount, boolean expectContinue, Action<? super RequestSpec> redirectRequestConfig) throws Exception;

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

  private static URI absolutizeRedirect(URI requestUri, String redirectLocation) throws URISyntaxException {
    //Rules
    //1. Given absolute URL use it
    //1a. Protocol Relative URL given starting of // we use the protocol from the request
    //2. Given Starting Slash prepend public facing domain:port if provided if not use base URL of request
    //3. Given relative URL prepend public facing domain:port plus parent path of request URL otherwise full parent path

    URI redirectLocationUri = URI.create(redirectLocation);
    if (redirectLocation.startsWith("http://") || redirectLocation.startsWith("https://")) { // absolute URI
      return redirectLocationUri;
    } else {
      if (redirectLocation.startsWith("//")) { // protocol relative
        return URI.create(requestUri.getScheme() + ":" + redirectLocation);
      } else {
        return absolutizeRelativeRedirect(requestUri, redirectLocationUri);
      }
    }
  }

  private static URI absolutizeRelativeRedirect(URI request, URI redirect) throws URISyntaxException {
    URI base = new URI(
        request.getScheme(),
        request.getUserInfo(),
        request.getHost(),
        request.getPort(),
        null,
        null,
        null
    );

    String path = redirect.getRawPath();
    if (!path.startsWith("/")) { // absolute path
      path = getParentPath(request.getRawPath()) + path;
    }

    String query = redirect.getRawQuery() == null
        ? ""
        : "?" + redirect.getRawQuery();

    return URI.create(base.toASCIIString() + path + query);
  }

  private static String getParentPath(String path) {
    String parentPath = "/";

    int indexOfSlash = path.lastIndexOf('/');
    if (indexOfSlash >= 0) {
      parentPath = path.substring(0, indexOfSlash) + '/';
    }

    if (!parentPath.startsWith("/")) {
      parentPath = "/" + parentPath;
    }
    return parentPath;
  }

}
