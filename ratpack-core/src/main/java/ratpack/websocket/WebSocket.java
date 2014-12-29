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

package ratpack.websocket;

import io.netty.buffer.ByteBuf;
import ratpack.api.NonBlocking;

public interface WebSocket {

  @NonBlocking
  void close();

  @NonBlocking
  void close(int statusCode, String reason);

  boolean isOpen();

  @NonBlocking
  void send(String text);

  @NonBlocking
  void send(ByteBuf text);

}
