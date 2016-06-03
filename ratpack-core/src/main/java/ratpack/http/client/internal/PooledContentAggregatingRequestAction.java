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

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.Downstream;
import ratpack.exec.Execution;
import ratpack.func.Action;
import ratpack.http.client.ReceivedResponse;
import ratpack.http.client.RequestSpec;

import java.net.URI;

public class PooledContentAggregatingRequestAction extends AbstractPooledRequestAction<ReceivedResponse> {

  public static final Logger LOGGER = LoggerFactory.getLogger(PooledContentAggregatingRequestAction.class);

  private int maxContentLengthBytes;

  public PooledContentAggregatingRequestAction(Action<? super RequestSpec> requestConfigurer,
                                               ChannelPoolMap<URI, ChannelPool> channelPoolMap,
                                               URI uri,
                                               ByteBufAllocator byteBufAllocator,
                                               int maxContentLengthBytes,
                                               Execution execution,
                                               int redirectCount) {
    super(requestConfigurer, channelPoolMap, uri, byteBufAllocator, execution, redirectCount);
    this.maxContentLengthBytes = maxContentLengthBytes;
  }

  @Override
  protected void addResponseHandlers(ChannelPipeline p, Downstream<? super ReceivedResponse> downstream) {
    addHandler(p, "aggregator", new HttpObjectAggregator(maxContentLengthBytes));
    addHandler(p, "httpResponseHandler", new SimpleChannelInboundHandler<FullHttpResponse>(false) {
      private boolean isKeepAlive;

      @Override
      protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        ctx.pipeline().remove(this);
        if (!(isKeepAlive = HttpUtil.isKeepAlive(msg)) && ctx.channel().isOpen()) {
          ctx.close();
        }
        channelPoolMap.get(baseURI).release(ctx.channel());
        success(downstream, toReceivedResponse(msg));
      }

      @Override
      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.pipeline().remove(this);
        if (!isKeepAlive) {
          ctx.close();
        }
        channelPoolMap.get(baseURI).release(ctx.channel());
        error(downstream, cause);
      }
    });
  }


  @Override
  protected AbstractPooledRequestAction<ReceivedResponse> buildRedirectRequestAction(Action<? super RequestSpec> redirectRequestConfig, URI locationUrl, int redirectCount) {
    return new PooledContentAggregatingRequestAction(redirectRequestConfig, channelPoolMap, locationUrl, byteBufAllocator, maxContentLengthBytes, execution, redirectCount);
  }
}
