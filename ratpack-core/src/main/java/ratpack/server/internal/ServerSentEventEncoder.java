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

package ratpack.server.internal;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.DefaultHttpContent;
import ratpack.http.ServerSentEvent;
import ratpack.util.internal.IoUtils;

import java.util.List;
import java.util.regex.Pattern;

public class ServerSentEventEncoder extends MessageToMessageEncoder<ServerSentEvent> {
  private static final Pattern PATTERN_NEW_LINE = Pattern.compile("\n");
  public static final String EVENT_TYPE_PREFIX = "event: ";
  public static final String EVENT_DATA_PREFIX = "data: ";
  public static final String EVENT_ID_PREFIX = "id: ";

  @Override
  protected void encode(ChannelHandlerContext ctx, ServerSentEvent msg, List<Object> out) throws Exception {
    StringBuilder eventBuilder = new StringBuilder();

    if (msg.getEventType() != null) {
      eventBuilder.append(EVENT_TYPE_PREFIX).append(msg.getEventType()).append('\n');
    }

    if (msg.getEventData() != null) {
      for (String dataLine : PATTERN_NEW_LINE.split(msg.getEventData())) {
        eventBuilder.append(EVENT_DATA_PREFIX).append(dataLine).append('\n');
      }
    }

    if (msg.getEventId() != null) {
      eventBuilder.append(EVENT_ID_PREFIX).append(msg.getEventId()).append('\n');
    }

    eventBuilder.append('\n');

    out.add(new DefaultHttpContent(IoUtils.utf8Buffer(eventBuilder.toString())));
  }
}
