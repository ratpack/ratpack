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

package ratpack.http.internal;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import ratpack.http.HttpResponseChunk;
import ratpack.util.internal.IoUtils;

public class DefaultChunkedResponseTransmitter extends StreamTransmitterSupport<HttpResponseChunk> implements ChunkedResponseTransmitter {

  public DefaultChunkedResponseTransmitter(FullHttpRequest request, HttpHeaders httpHeaders, Channel channel) {
    super(request, httpHeaders, channel);
  }

  @Override
  protected void setResponseHeaders(HttpResponse response) {
    response.headers().set("Content-Length", 0);
    response.headers().set("Transfer-Encoding", "chunked");

    super.setResponseHeaders(response);
  }

  @Override
  protected void doOnComplete() {
    ChannelFuture writeFuture = channel.writeAndFlush(IoUtils.utf8Buffer("0\r\n\r\n"));
    writeFuture.addListener(new ChannelFutureListener() {
      public void operationComplete(ChannelFuture future) throws Exception {
        if (!future.isSuccess()) {
          channel.close();
        }
      }
    });

    super.doOnComplete();
  }
}