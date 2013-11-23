/*
 * Copyright 2013 the original author or authors.
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

package ratpack.codahale;

import com.codahale.metrics.Timer;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

import static ratpack.codahale.MetricsModule.registry;

public class MetricChannelHandler extends ChannelDuplexHandler {

  private Timer.Context timerContext;

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof FullHttpRequest) {
      FullHttpRequest request = (FullHttpRequest)msg;
      String tag = buildRequestTimerTag(request.getUri(), request.getMethod().name());
      timerContext = registry.timer(tag).time();
    }

    super.channelRead(ctx, msg);
  }

  @Override
  public void flush(ChannelHandlerContext ctx) throws Exception {
    if (timerContext != null) timerContext.stop();
    super.flush(ctx);
  }

  private String buildRequestTimerTag(String requestUri, String requestMethod) {
    StringBuilder tag = new StringBuilder();
    tag.append(requestUri.equals("/") ? "[root" : requestUri.replaceFirst("/", "[").replace("/", "]["));
    tag.append("]~");
    tag.append(requestMethod);
    tag.append("~Request");
    return tag.toString();
  }

}
