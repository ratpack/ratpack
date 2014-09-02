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

/**
 * Represents a Server Sent Event.
 *
 * @see ServerSentEvents
 */
public class ServerSentEvent {

  /**
   * Returns a new {@link Builder} for {@link ratpack.sse.ServerSentEvent}.
   *
   * @return a {@link Builder} instance for a {@link ratpack.sse.ServerSentEvent}
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder for {@link ratpack.sse.ServerSentEvent} instances.
   */
  public static class Builder {
    private String eventId;
    private String eventType;
    private String eventData;

    /**
     * Specify the event id for the server sent event.
     *
     * @param id the event id
     * @return this {@link ratpack.sse.ServerSentEvent.Builder}
     */
    public Builder id(String id) {
      this.eventId = id;
      return this;
    }

    /**
     * Specify the event type for the server sent event.
     *
     * @param type the event type
     * @return this {@link ratpack.sse.ServerSentEvent.Builder}
     */
    public Builder type(String type) {
      this.eventType = type;
      return this;
    }

    /**
     * Specify the event data for the server sent event.
     *
     * @param data the event data
     * @return this {@link ratpack.sse.ServerSentEvent.Builder}
     */
    public Builder data(String data) {
      this.eventData = data;
      return this;
    }

    /**
     * Builds a {@link ratpack.sse.ServerSentEvent} with the given properties.
     *
     * @return a {@link ratpack.sse.ServerSentEvent}
     */
    public ServerSentEvent build() {
      return new ServerSentEvent(eventId, eventType, eventData);
    }
  }

  private final String eventId;
  private final String eventType;
  private final String eventData;

  private ServerSentEvent(String eventId, String eventType, String eventData) {
    if (eventId == null && eventType == null && eventData == null) {
      throw new IllegalArgumentException("You must supply at least one of evenId, eventType, eventData");
    }

    this.eventId = eventId;
    this.eventType = eventType;
    this.eventData = eventData;
  }

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
