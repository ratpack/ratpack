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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.Downstream;
import ratpack.exec.Execution;
import ratpack.func.Action;
import ratpack.http.Headers;
import ratpack.http.MutableHeaders;
import ratpack.http.Response;
import ratpack.http.Status;
import ratpack.http.client.RequestSpec;
import ratpack.http.client.StreamedResponse;
import ratpack.http.internal.DefaultStatus;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.http.internal.NettyHeadersBackedHeaders;
import ratpack.stream.Streams;
import ratpack.stream.TransformablePublisher;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import static ratpack.util.Exceptions.uncheck;

public class PooledContentStreamingRequestAction extends AbstractPooledRequestAction<StreamedResponse> {

  public static final Logger LOGGER = LoggerFactory.getLogger(PooledContentStreamingRequestAction.class);

  private final AtomicBoolean subscribedTo = new AtomicBoolean();


  public PooledContentStreamingRequestAction(Action<? super RequestSpec> requestConfigurer, ChannelPoolMap<URI, ChannelPool> channelPoolMap, URI uri, ByteBufAllocator byteBufAllocator, Execution execution, int redirectCount) {
    super(requestConfigurer, channelPoolMap, uri, byteBufAllocator, execution, redirectCount);
  }

  @Override
  protected void addResponseHandlers(ChannelPipeline p, Downstream<? super StreamedResponse> downstream) {
    addHandler(p, "httpResponseHandler", new SimpleChannelInboundHandler<HttpResponse>(false) {
      private boolean isKeepAlive;

      @Override
      protected void channelRead0(ChannelHandlerContext ctx, HttpResponse msg) throws Exception {
        isKeepAlive = HttpUtil.isKeepAlive(msg);
        // Switch auto reading off so we can control the flow of response content
        p.channel().config().setAutoRead(false);
        execution.onComplete(() -> {
          channelPoolMap.get(baseURI).release(ctx.channel());
          if (!subscribedTo.get() && ctx.channel().isOpen()) {
            if (!isKeepAlive) {
              ctx.close();
            }
          }
        });

        final Headers headers = new NettyHeadersBackedHeaders(msg.headers());
        final Status status = new DefaultStatus(msg.status());

        success(downstream, new DefaultStreamedResponse(p, status, headers, isKeepAlive));
      }

      @Override
      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        channelPoolMap.get(baseURI).release(ctx.channel());
        if (!isKeepAlive) {
          LOGGER.error("Closing channel={}", ctx.channel().id().asShortText(), cause);
          ctx.close();
        }
        error(downstream, cause);
      }
    });
  }

  @Override
  protected AbstractPooledRequestAction<StreamedResponse> buildRedirectRequestAction(Action<? super RequestSpec> redirectRequestConfig, URI locationUrl, int redirectCount) {
    return new PooledContentStreamingRequestAction(redirectRequestConfig, channelPoolMap, locationUrl, byteBufAllocator, execution, redirectCount);
  }

  private class DefaultStreamedResponse implements StreamedResponse {
    private final ChannelPipeline channelPipeline;
    private final Status status;
    private final Headers headers;
    private final boolean isKeepAlive;

    public DefaultStreamedResponse(ChannelPipeline p, Status status, Headers headers, boolean isKeepAlive) {
      this.channelPipeline = p;
      this.status = status;
      this.headers = headers;
      this.isKeepAlive = isKeepAlive;
    }

    @Override
    public Status getStatus() {
      return status;
    }

    @Override
    public int getStatusCode() {
      return status.getCode();
    }

    @Override
    public Headers getHeaders() {
      return headers;
    }

    @Override
    public TransformablePublisher<ByteBuf> getBody() {
      return Streams.transformable(new HttpContentPublisher(channelPipeline, isKeepAlive));
    }

    @Override
    public void forwardTo(Response response) {
      forwardTo(response, Action.noop());
    }

    @Override
    public void forwardTo(Response response, Action<? super MutableHeaders> headerMutator) {
      response.getHeaders().copy(this.headers);
      response.getHeaders().remove(HttpHeaderConstants.CONTENT_LENGTH); // responses will always be chunked
      try {
        headerMutator.execute(response.getHeaders());
      } catch (Exception e) {
        throw uncheck(e);
      }
      response.getHeaders().set(HttpHeaderConstants.TRANSFER_ENCODING, HttpHeaderConstants.CHUNKED);

      response.status(this.status);
      response.sendStream(getBody());
    }
  }

  private class HttpContentPublisher implements Publisher<ByteBuf> {
    private Subscriber<? super ByteBuf> subscriber;
    private final ChannelPipeline channelPipeline;
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final boolean isKeepAlive;

    public HttpContentPublisher(ChannelPipeline p, boolean isKeepAlive) {
      this.channelPipeline = p;
      this.isKeepAlive = isKeepAlive;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuf> s) {
      subscribedTo.compareAndSet(false, true);
      subscriber = s;

      addHandler(channelPipeline, "httpContentHandler", new SimpleChannelInboundHandler<HttpContent>(false) {
        @Override
        public void channelRead0(ChannelHandlerContext ctx, HttpContent msg) throws Exception {
          subscriber.onNext(msg.content());
          if (msg instanceof LastHttpContent && stopped.compareAndSet(false, true)) {
            subscriber.onComplete();
          }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
          if (stopped.compareAndSet(false, true)) {
            subscriber.onError(cause);
          }
          channelPoolMap.get(baseURI).release(ctx.channel());
          if (!isKeepAlive && ctx.channel().isOpen()) {
            ctx.close();
          }
        }
      });

      s.onSubscribe(new Subscription() {
        @Override
        public void request(long n) {
          if (n < 1) {
            throw new IllegalArgumentException("3.9 While the Subscription is not cancelled, Subscription.request(long n) MUST throw a java.lang.IllegalArgumentException if the argument is <= 0.");
          }

          if (!stopped.get()) {
            if (n == Long.MAX_VALUE) {
              channelPipeline.channel().config().setAutoRead(true);
            } else {
              for (int i = 0; i < n; i++) {
                channelPipeline.channel().read();
              }
            }
          }
        }

        @Override
        public void cancel() {
          stopped.set(true);
          channelPoolMap.get(baseURI).release(channelPipeline.channel());
          if (!isKeepAlive && channelPipeline.channel().isOpen()) {
            channelPipeline.channel().close();
          }
        }
      });
    }
  }

}
