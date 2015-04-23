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
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpContentDecompressor;
import ratpack.exec.Execution;
import ratpack.exec.Fulfiller;
import ratpack.func.Action;
import ratpack.http.Headers;
import ratpack.http.Status;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;
import ratpack.http.internal.*;

import java.net.URI;

class ContentAggregatingRequestAction extends RequestActionSupport<ReceivedResponse> {

  private final int maxContentLengthBytes;

  public ContentAggregatingRequestAction(Action<? super RequestSpec> requestConfigurer, URI uri, Execution execution, ByteBufAllocator byteBufAllocator, int maxContentLengthBytes) {
    super(requestConfigurer, uri, execution, byteBufAllocator);
    this.maxContentLengthBytes = maxContentLengthBytes;
  }

  @Override
  protected void addResponseHandlers(ChannelPipeline p, Fulfiller<? super ReceivedResponse> fulfiller) {
    p.addLast(new HttpContentDecompressor());
    p.addLast("aggregator", new HttpObjectAggregator(maxContentLengthBytes));
    p.addLast("httpResponseHandler", new SimpleChannelInboundHandler<FullHttpResponse>(false) {
      @Override
      public void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        final Headers headers = new NettyHeadersBackedHeaders(msg.headers());
        String contentType = headers.get(HttpHeaderConstants.CONTENT_TYPE.toString());
        ByteBuf responseBuffer = initBufferReleaseOnExecutionClose(msg.content(), execution);
        final ByteBufBackedTypedData typedData = new ByteBufBackedTypedData(responseBuffer, DefaultMediaType.get(contentType));
        final Status status = new DefaultStatus(msg.status());

        success(fulfiller, new DefaultReceivedResponse(status, headers, typedData));
      }

      @Override
      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        error(fulfiller, cause);
      }
    });
  }

  @Override
  protected RequestActionSupport<ReceivedResponse> buildRedirectRequestAction(Action<? super RequestSpec> redirectRequestConfig, URI locationUrl) {
    return new ContentAggregatingRequestAction(redirectRequestConfig, locationUrl, execution, byteBufAllocator, maxContentLengthBytes);
  }

  private static ByteBuf initBufferReleaseOnExecutionClose(final ByteBuf responseBuffer, Execution execution) {
    execution.onCleanup(responseBuffer::release);
    return responseBuffer;
  }

}
