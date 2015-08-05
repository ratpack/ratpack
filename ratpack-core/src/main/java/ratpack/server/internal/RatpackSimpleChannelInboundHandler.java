/*
 * Copyright 2015 the original author or authors.
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

package ratpack.server.internal;

import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpRequest;

public abstract class RatpackSimpleChannelInboundHandler extends SimpleChannelInboundHandler<HttpRequest> {

  public RatpackSimpleChannelInboundHandler() {
  }

  public RatpackSimpleChannelInboundHandler(boolean autoRelease) {
    super(autoRelease);
  }

  public RatpackSimpleChannelInboundHandler(Class<? extends HttpRequest> inboundMessageType) {
    super(inboundMessageType);
  }

  public RatpackSimpleChannelInboundHandler(Class<? extends HttpRequest> inboundMessageType, boolean autoRelease) {
    super(inboundMessageType, autoRelease);
  }
}