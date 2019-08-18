/*
 * Copyright 2017 the original author or authors.
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

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

public class ConnectionClosureReason {

  private static final AttributeKey<String> ATTRIBUTE_KEY = AttributeKey.valueOf(ConnectionClosureReason.class, "string");

  private ConnectionClosureReason() {
  }

  public static void setIdle(Channel channel) {
    channel.attr(ATTRIBUTE_KEY).set("Connection to " + channel.remoteAddress().toString() + " was closed due to idle timeout");
  }

  public static String get(Channel channel) {
    String reason = channel.attr(ATTRIBUTE_KEY).get();
    if (reason == null) {
      return "Remote " + channel.remoteAddress() + " closed the connection";
    } else {
      return reason;
    }
  }
}
