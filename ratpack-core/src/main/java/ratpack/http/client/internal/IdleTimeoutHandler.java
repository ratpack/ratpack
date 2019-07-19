/*
 * Copyright 2018 the original author or authors.
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

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import ratpack.http.client.IdleTimeoutAction;

@ChannelHandler.Sharable
public class IdleTimeoutHandler extends ChannelDuplexHandler {

  private final IdleTimeoutAction idleTimeoutAction;

  public static IdleTimeoutHandler newInstance(IdleTimeoutAction idleTimeoutAction) {
    return new IdleTimeoutHandler(idleTimeoutAction);
  }

  private IdleTimeoutHandler(IdleTimeoutAction idleTimeoutAction) {
    this.idleTimeoutAction = idleTimeoutAction;
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      IdleStateEvent e = (IdleStateEvent) evt;
      if (e.state() == IdleState.READER_IDLE || e.state() == IdleState.WRITER_IDLE) {
        idleTimeoutAction.execute(ctx);
      }
    }
  }
}
