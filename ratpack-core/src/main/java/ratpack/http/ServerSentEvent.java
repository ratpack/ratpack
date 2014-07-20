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

package ratpack.http;

import io.netty.buffer.ByteBuf;
import ratpack.util.internal.IoUtils;

import java.util.regex.Pattern;

public class ServerSentEvent implements StreamElement {

  private static final Pattern PATTERN_NEW_LINE = Pattern.compile("\n");

  private final String eventId;
  private final String eventType;
  private final String eventData;

  public ServerSentEvent(String eventId, String eventType, String eventData) {
    this.eventId = eventId;
    this.eventType = eventType;
    this.eventData = eventData;
  }

  @Override
  public ByteBuf getValue() {
    StringBuilder eventBuilder = new StringBuilder();

    if (this.eventType != null) {
      eventBuilder.append("event: ").append(this.eventType).append('\n');
    }

    if (this.eventData != null) {
      for (String dataLine : PATTERN_NEW_LINE.split(this.eventData)) {
        eventBuilder.append("data: ").append(dataLine).append('\n');
      }
    }

    if (this.eventId != null) {
      eventBuilder.append("id: ").append(this.eventId).append('\n');
    }

    eventBuilder.append('\n');

    return IoUtils.utf8Buffer(eventBuilder.toString());
  }
}
