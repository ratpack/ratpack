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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import ratpack.exec.Execution;
import ratpack.exec.Fulfiller;
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

class ContentStreamingRequestAction extends RequestActionSupport<StreamedResponse> {
  private final AtomicBoolean subscribedTo = new AtomicBoolean();

  public ContentStreamingRequestAction(Action<? super RequestSpec> requestConfigurer, URI uri, Execution execution, ByteBufAllocator byteBufAllocator) {
    super(requestConfigurer, uri, execution, byteBufAllocator);
  }

  @Override
  protected RequestActionSupport<StreamedResponse> buildRedirectRequestAction(Action<? super RequestSpec> redirectRequestConfig, URI locationUrl) {
    return new ContentStreamingRequestAction(redirectRequestConfig, locationUrl, execution, byteBufAllocator);
  }

  @Override
  protected void addResponseHandlers(ChannelPipeline p, Fulfiller<? super StreamedResponse> fulfiller) {
    p.addLast("httpResponseHandler", new SimpleChannelInboundHandler<HttpResponse>(false) {
      @Override
      public void channelRead0(ChannelHandlerContext ctx, HttpResponse msg) throws Exception {
        // Switch auto reading off so we can control the flow of response content
        p.channel().config().setAutoRead(false);
        execution.onCleanup(() -> {
          if (!subscribedTo.get() && ctx.channel().isOpen()) {
            ctx.close();
          }
        });

        final Headers headers = new NettyHeadersBackedHeaders(msg.headers());
        final Status status = new DefaultStatus(msg.status());

        success(fulfiller, new DefaultStreamedResponse(p, status, headers));
      }

      @Override
      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        error(fulfiller, cause);
      }
    });
  }

  private class DefaultStreamedResponse implements StreamedResponse {
    private final ChannelPipeline channelPipeline;
    private final Status status;
    private final Headers headers;

    public DefaultStreamedResponse(ChannelPipeline p, Status status, Headers headers) {
      this.channelPipeline = p;
      this.status = status;
      this.headers = headers;
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
      return Streams.transformable(new HttpContentPublisher(channelPipeline));
    }

    @Override
    public void send(Response response) {
      send(response, null);
    }

    @Override
    public void send(Response response, Action<? super MutableHeaders> headerMutator) {
      response.getHeaders().add(HttpHeaderConstants.TRANSFER_ENCODING, HttpHeaderConstants.CHUNKED);
      response.getHeaders().set(HttpHeaderConstants.CONTENT_TYPE, this.headers.get(HttpHeaderConstants.CONTENT_TYPE));
      response.sendStream(getBody());
    }
  }

  private class HttpContentPublisher implements Publisher<ByteBuf> {
    private Subscriber<? super ByteBuf> subscriber;
    private final ChannelPipeline channelPipeline;
    private final AtomicBoolean stopped = new AtomicBoolean();

    public HttpContentPublisher(ChannelPipeline p) {
      this.channelPipeline = p;
    }

    @Override
    public void subscribe(Subscriber<? super ByteBuf> s) {
      subscribedTo.compareAndSet(false, true);
      subscriber = s;

      channelPipeline.remove("httpResponseHandler");
      channelPipeline.addLast("httpContentHandler", new SimpleChannelInboundHandler<HttpContent>(false) {
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

          if (ctx.channel().isOpen()) {
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
            for (int i = 0; i < n; i++) {
              channelPipeline.channel().read();
            }
          }
        }

        @Override
        public void cancel() {
          stopped.set(true);
          channelPipeline.channel().close();
        }
      });
    }
  }

}
