/*
 * Copyright 2019 the original author or authors.
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

package ratpack.http.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import ratpack.func.Action;

/**
 * An interface that allows the definition of a configurable action to take if a connection in
 * the {@link HttpClient} connection pool becomes idle. close and heartbeat actions have been provided.
 *
 * @since 1.8.0
 */
public interface IdleTimeoutAction extends Action<ChannelHandlerContext> {

  /**
   * Close the connection on idle.
   *
   * @return IdleTimeoutAction
   */
  static IdleTimeoutAction close() {
    return ChannelHandlerContext::close;
  }

  /**
   * Send a heartbeat to the connection on idle.
   *
   * @return IdleTimeoutAction
   */
  static IdleTimeoutAction heartbeat() {
    return ctx -> {
      ByteBuf ping = Unpooled.wrappedBuffer("ping".getBytes());
      ctx.writeAndFlush(ping).addListener(f -> {
        if (!f.isSuccess()) {
          ctx.close();
        }
      });
    };
  }
}
