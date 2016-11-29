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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCounted;
import org.reactivestreams.Subscription;
import ratpack.exec.Downstream;
import ratpack.exec.Execution;
import ratpack.exec.Upstream;
import ratpack.func.Action;
import ratpack.http.Headers;
import ratpack.http.MutableHeaders;
import ratpack.http.Response;
import ratpack.http.Status;
import ratpack.http.client.RequestSpec;
import ratpack.http.client.StreamedResponse;
import ratpack.http.internal.DefaultStatus;
import ratpack.http.internal.NettyHeadersBackedHeaders;
import ratpack.stream.TransformablePublisher;
import ratpack.stream.internal.BufferedWriteStream;
import ratpack.stream.internal.BufferingPublisher;
import ratpack.util.Exceptions;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ContentStreamingRequestAction extends RequestActionSupport<StreamedResponse> {

  private static final String HANDLER_NAME = "streaming";

  ContentStreamingRequestAction(URI uri, HttpClientInternal client, int redirectCount, Execution execution, Action<? super RequestSpec> requestConfigurer) {
    super(uri, client, redirectCount, execution, requestConfigurer);
  }

  @Override
  protected void doDispose(ChannelPipeline channelPipeline, boolean forceClose) {
    channelPipeline.remove(HANDLER_NAME);
    super.doDispose(channelPipeline, forceClose);
  }

  @Override
  protected void addResponseHandlers(ChannelPipeline p, Downstream<? super StreamedResponse> downstream) {
    p.addLast(HANDLER_NAME, new Handler(p, downstream));
  }

  @Override
  protected Upstream<StreamedResponse> onRedirect(URI locationUrl, int redirectCount, Action<? super RequestSpec> redirectRequestConfig) {
    return new ContentStreamingRequestAction(locationUrl, client, redirectCount, execution, redirectRequestConfig);
  }

  private class Handler extends SimpleChannelInboundHandler<HttpObject> {

    private final ChannelPipeline channelPipeline;
    private final Downstream<? super StreamedResponse> downstream;

    private List<HttpContent> received;
    private BufferedWriteStream<ByteBuf> write;
    private HttpResponse response;

    public Handler(ChannelPipeline channelPipeline, Downstream<? super StreamedResponse> downstream) {
      super(false);
      this.channelPipeline = channelPipeline;
      this.downstream = downstream;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject httpObject) throws Exception {
      if (httpObject instanceof HttpResponse) {
        this.response = (HttpResponse) httpObject;
        // Switch auto reading off so we can control the flow of response content
        channelPipeline.channel().config().setAutoRead(false);
        execution.onComplete(() -> {
          if (write == null) {
            forceDispose(channelPipeline);
          }
          if (received != null) {
            received.forEach(ReferenceCounted::release);
          }
        });
        StreamedResponse response = new DefaultStreamedResponse(channelPipeline);
        client.getResponseInterceptor().execute(response);
        success(downstream, response);
      } else if (httpObject instanceof HttpContent) {
        HttpContent httpContent = ((HttpContent) httpObject).touch();
        if (write == null) {
          if (received == null) {
            received = new ArrayList<>();
          }
          received.add(httpContent.touch());
          if (httpObject instanceof LastHttpContent) {
            dispose(ctx.pipeline(), response);
          }
        } else {
          if (httpContent.content().readableBytes() > 0) {
            write.item(httpContent.content().touch("emitting to user code"));
          } else {
            httpContent.release();
          }
          if (httpObject instanceof LastHttpContent) {
            dispose(ctx.pipeline(), response);
            write.complete();
          } else {
            if (write.getRequested() > 0) {
              ctx.read();
            }
          }
        }
      }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      forceDispose(ctx.pipeline());
      error(downstream, cause);
    }

    class DefaultStreamedResponse implements StreamedResponse {
      private final ChannelPipeline channelPipeline;
      private final Status status;
      private final Headers headers;

      private DefaultStreamedResponse(ChannelPipeline channelPipeline) {
        this.channelPipeline = channelPipeline;
        this.headers = new NettyHeadersBackedHeaders(response.headers());
        this.status = new DefaultStatus(response.status());
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
        return new BufferingPublisher<>(ByteBuf::release, write -> {
          Handler.this.write = write;

          if (received != null) {
            for (HttpContent httpContent : received) {
              if (httpContent.content().readableBytes() > 0) {
                write.item(httpContent.content().touch("emitting to user code"));
              } else {
                httpContent.release();
              }
              if (httpContent instanceof LastHttpContent) {
                dispose(channelPipeline, response);
                write.complete();
              }
            }
          }

          return new Subscription() {
            @Override
            public void request(long n) {
              channelPipeline.read();
            }

            @Override
            public void cancel() {
              forceDispose(channelPipeline);
            }
          };
        });

      }

      @Override
      public void forwardTo(Response response) {
        forwardTo(response, Action.noop());
      }

      @Override
      public void forwardTo(Response response, Action<? super MutableHeaders> headerMutator) {
        MutableHeaders outgoingHeaders = response.getHeaders();
        outgoingHeaders.copy(headers);
        outgoingHeaders.remove(HttpHeaderNames.CONNECTION);
        Exceptions.uncheck(() -> headerMutator.execute(outgoingHeaders));
        response.status(status);

        response.sendStream(getBody().bindExec(ByteBuf::release));
      }

    }
  }
}
