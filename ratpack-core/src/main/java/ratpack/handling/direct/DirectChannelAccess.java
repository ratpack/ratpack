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

package ratpack.handling.direct;

import io.netty.channel.Channel;
import ratpack.func.Action;

/**
 * Provides direct access to the underlying Netty {@link io.netty.channel.Channel}.
 * <p>
 * Use of this type is discouraged.
 * It exists to provide low level integration with Netty for extra protocols to build on, such as the {@link ratpack.websocket.WebSockets} support.
 */
public interface DirectChannelAccess {

  /**
   * The channel.
   *
   * @return The backing Netty channel for the request.
   */
  Channel getChannel();

  /**
   * Signals that Ratpack should no longer manage this channel.
   * <p>
   * All future messages received over the channel will be given to the provided action.
   * It is the caller of this method's responsibility for closing the channel after it is finished.
   *
   * @param messageReceiver the responder to channel messages.
   */
  void takeOwnership(Action<Object> messageReceiver);

}
