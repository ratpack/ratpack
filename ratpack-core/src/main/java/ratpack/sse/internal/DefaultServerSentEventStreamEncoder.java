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

package ratpack.sse.internal;

import org.reactivestreams.Publisher;
import ratpack.sse.ServerSentEvent;
import ratpack.stream.internal.StreamProcessorSupport;
import ratpack.util.internal.IoUtils;

import java.util.regex.Pattern;

public class DefaultServerSentEventStreamEncoder extends StreamProcessorSupport<ServerSentEvent> implements ServerSentEventStreamEncoder {
  private static final Pattern PATTERN_NEW_LINE = Pattern.compile("\n");
  private static final String EVENT_TYPE_PREFIX = "event: ";
  private static final String EVENT_DATA_PREFIX = "data: ";
  private static final String EVENT_ID_PREFIX = "id: ";

  public DefaultServerSentEventStreamEncoder(Publisher<ServerSentEvent> sourcePublisher) {
    super(sourcePublisher);
  }

  @Override
  public void onNext(ServerSentEvent serverSentEvent) {
    StringBuilder eventBuilder = new StringBuilder();

    if (serverSentEvent.getEventType() != null) {
      eventBuilder.append(EVENT_TYPE_PREFIX).append(serverSentEvent.getEventType()).append('\n');
    }

    if (serverSentEvent.getEventData() != null) {
      for (String dataLine : PATTERN_NEW_LINE.split(serverSentEvent.getEventData())) {
        eventBuilder.append(EVENT_DATA_PREFIX).append(dataLine).append('\n');
      }
    }

    if (serverSentEvent.getEventId() != null) {
      eventBuilder.append(EVENT_ID_PREFIX).append(serverSentEvent.getEventId()).append('\n');
    }

    eventBuilder.append('\n');

    this.targetSubscriber.onNext(IoUtils.utf8Buffer(eventBuilder.toString()));
  }

}
