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

package ratpack.sse;

import java.util.function.Consumer;

/**
 * Represents a Server Sent Event.
 *
 * @see ServerSentEvents
 */
public class ServerSentEvent {

  /**
   * Returns a new event, built by the given spec consumer.
   *
   * @return a new event
   */
  public static ServerSentEvent serverSentEvent(Consumer<? super Spec> consumer) {
    ServerSentEvent event = new ServerSentEvent();
    consumer.accept(new Spec() {
      @Override
      public Spec id(String id) {
        event.eventId = id;
        return this;
      }

      @Override
      public Spec event(String eventType) {
        event.eventType = eventType;
        return this;
      }

      @Override
      public Spec data(String data) {
        event.eventData = data;
        return this;
      }
    });

    if (event.eventId == null && event.eventType == null && event.eventData == null) {
      throw new IllegalArgumentException("You must supply at least one of data, event, id");
    }

    return event;
  }

  /**
   * A builder for {@link ratpack.sse.ServerSentEvent} instances.
   */
  public static interface Spec {

    /**
     * Specify the event id for the server sent event.
     *
     * @param id the event id
     * @return this
     */
    Spec id(String id);

    /**
     * Specify the event type for the server sent event.
     *
     * @param event the event type
     * @return this
     */
    Spec event(String event);

    /**
     * Specify the event data for the server sent event.
     *
     * @param data the event data
     * @return this
     */
    Spec data(String data);

  }

  private String eventId;
  private String eventType;
  private String eventData;

  public String getEventId() {
    return eventId;
  }

  public String getEventType() {
    return eventType;
  }

  public String getEventData() {
    return eventData;
  }
}
