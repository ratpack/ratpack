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
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import ratpack.http.ServerSentEvent;

public class DefaultServerSentEventTransmitter extends StreamTransmitterSupport<ServerSentEvent> implements ServerSentEventTransmitter {

  public DefaultServerSentEventTransmitter(FullHttpRequest request, HttpHeaders httpHeaders, Channel channel) {
    super(request, httpHeaders, channel);
  }

  @Override
  protected void setResponseHeaders(HttpResponse response) {
    response.headers().set("Content-Type", "text/event-stream;charset=UTF-8");
    response.headers().set("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
    response.headers().set("Pragma", "no-cache");

    super.setResponseHeaders(response);
  }
}
