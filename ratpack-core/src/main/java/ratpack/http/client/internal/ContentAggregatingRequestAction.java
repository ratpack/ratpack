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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObjectAggregator;
import ratpack.exec.Downstream;
import ratpack.exec.Execution;
import ratpack.func.Action;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;

import java.net.URI;

class ContentAggregatingRequestAction extends RequestActionSupport<ReceivedResponse> {

  ContentAggregatingRequestAction(
    Action<? super RequestSpec> requestConfigurer,
    URI uri,
    HttpClientInternal client,
    Execution execution,
    int redirectCount
  ) {
    super(requestConfigurer, uri, client, execution, redirectCount);
  }

  @Override
  protected void addResponseHandlers(ChannelPipeline p, Downstream<? super ReceivedResponse> downstream) {
    addHandler(p, "aggregator", new HttpObjectAggregator(client.getMaxContentLength()));
    addHandler(p, "httpResponseHandler", new SimpleChannelInboundHandler<FullHttpResponse>(false) {

      @Override
      protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        channelPool.release(ctx.channel());
        success(downstream, toReceivedResponse(msg));
      }

      @Override
      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        channelPool.release(ctx.channel());
        ctx.close();
        error(downstream, cause);
      }
    });
  }


  @Override
  protected RequestActionSupport<ReceivedResponse> buildRedirectRequestAction(Action<? super RequestSpec> redirectRequestConfig, URI locationUrl, int redirectCount) {
    return new ContentAggregatingRequestAction(redirectRequestConfig, locationUrl, client, execution, redirectCount);
  }
}
