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
   * @param consumer the event definition
   * @return a new event
   */
  public static ServerSentEvent serverSentEvent(Consumer<? super Spec<?>> consumer) {
    return serverSentEvent(null, consumer);
  }

  /**
   * Returns a new event, built by the given spec consumer with a supporting object for construction.
   *
   * @param item the supporting object
   * @param consumer the event definition
   * @param <T> the type of supporting object
   * @return a new event
   */
  public static <T> ServerSentEvent serverSentEvent(T item, Consumer<? super Spec<T>> consumer) {
    ServerSentEvent event = new ServerSentEvent();
    consumer.accept(new Spec<T>() {
      @Override
      public T getItem() {
        return item;
      }

      @Override
      public Spec<T> id(String id) {
        event.eventId = id;
        return this;
      }

      @Override
      public Spec<T> event(String eventType) {
        event.eventType = eventType;
        return this;
      }

      @Override
      public Spec<T> data(String data) {
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
  public static interface Spec<T> {

    /**
     * An object which can support the construction of the server sent event.
     *
     * @return a supporting object
     */
    T getItem();

    /**
     * Specify the event id for the server sent event.
     *
     * @param id the event id
     * @return this
     */
    Spec<T> id(String id);

    /**
     * Specify the event type for the server sent event.
     *
     * @param event the event type
     * @return this
     */
    Spec<T> event(String event);

    /**
     * Specify the event data for the server sent event.
     *
     * @param data the event data
     * @return this
     */
    Spec<T> data(String data);

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
