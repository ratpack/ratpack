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

package ratpack.core.http.client.internal;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import ratpack.core.bytebuf.ByteBufRef;
import ratpack.core.http.client.ReceivedResponse;
import ratpack.core.http.client.RequestSpec;
import ratpack.exec.Downstream;
import ratpack.exec.Execution;
import ratpack.exec.Upstream;
import ratpack.func.Action;

import java.net.URI;

class ContentAggregatingRequestAction extends RequestActionSupport<ReceivedResponse> {

  private static final String AGGREGATOR_HANDLER_NAME = "aggregator";
  private static final String RESPONSE_HANDLER_NAME = "response";

  ContentAggregatingRequestAction(
    URI uri,
    HttpClientInternal client,
    int redirectCount,
    boolean expectContinue,
    Execution execution,
    Action<? super RequestSpec> requestConfigurer
  ) throws Exception {
    super(uri, client, redirectCount, expectContinue, execution, requestConfigurer);
  }

  @Override
  protected void doDispose(ChannelPipeline channelPipeline, boolean forceClose) {
    channelPipeline.remove(AGGREGATOR_HANDLER_NAME);
    channelPipeline.remove(RESPONSE_HANDLER_NAME);
    super.doDispose(channelPipeline, forceClose);
  }

  @Override
  protected void addResponseHandlers(ChannelPipeline p, Downstream<? super ReceivedResponse> downstream) {
    p.addLast(AGGREGATOR_HANDLER_NAME, new NoContentLengthOnNoBodyHttpObjectAggregator(requestConfig.maxContentLength));
    p.addLast(RESPONSE_HANDLER_NAME, new SimpleChannelInboundHandler<FullHttpResponse>(false) {

      @Override
      protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {
        response.touch();
        dispose(ctx.pipeline(), response);

        ByteBuf content = new ByteBufRef(response.content());
        execution.onComplete(() -> {
          if (content.refCnt() > 0) {
            content.release();
          }
        });
        success(downstream, toReceivedResponse(response, content));
      }

      @Override
      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause = decorateException(cause);
        error(downstream, cause);
        forceDispose(ctx.pipeline());
      }
    });
  }

  @Override
  protected Upstream<ReceivedResponse> onRedirect(URI locationUrl, int redirectCount, boolean expectContinue, Action<? super RequestSpec> redirectRequestConfig) throws Exception {
    return new ContentAggregatingRequestAction(locationUrl, client, redirectCount, expectContinue, execution, redirectRequestConfig);
  }

}
