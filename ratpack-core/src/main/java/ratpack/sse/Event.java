/*
 * Copyright 2015 the original author or authors.
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

import ratpack.func.Function;

/**
 * An individual event in a server sent event stream.
 * <p>
 * The {@link #getItem() item} is the item in the data stream being emitted as a server sent event.
 * It can be used to derive values for the {@link #id}, {@link #event} and/or {@link #data} fields.
 * <p>
 * By default, the {@code id}, {@code event} and {@code data} fields are set to {@code null}.
 *
 * @param <T> the item type contained in the event
 * @see ratpack.sse.ServerSentEvents#serverSentEvents
 */
public interface Event<T> {
  /**
   * The stream item that this event.
   *
   * @return a supporting object
   */
  T getItem();

  /**
   * The “id” value of the event.
   * <p>
   * {@code null} by default.
   *
   * @return the “id” value of the event
   */
  String getId();

  /**
   * The “event” value of the event.
   * <p>
   * {@code null} by default.
   *
   * @return the “event” value of the event
   */
  String getEvent();

  /**
   * The “data” value of the event.
   * <p>
   * {@code null} by default.
   *
   * @return the “data” value of the event
   */
  String getData();

  /**
   * The comment for this event.
   * <p>
   * {@code null} by default.
   *
   * @return he comment for this event
   * @since 1.5
   */
  String getComment();

  /**
   * Sets the “id” value of the event to the return value of the given function.
   * <p>
   * The function receives the {@link #getItem() item} and is executed immediately.
   * <p>
   * The returned value must not contain a {@code '\n'} character as this is not valid in an event value.
   *
   * @param function a generator for the “id” value of the event
   * @return this
   * @throws Exception any thrown by {@code function}
   */
  Event<T> id(Function<? super T, String> function) throws Exception;

  /**
   * Specify the event id for the server sent event.
   * <p>
   * The value must not contain a {@code '\n'} character as this is not valid in an event value.
   *
   * @param id the event id
   * @return this
   */
  Event<T> id(String id);

  /**
   * Sets the “event” value of the event to the return value of the given function.
   * <p>
   * The function receives the {@link #getItem() item} and is executed immediately.
   * <p>
   * The returned value must not contain a {@code '\n'} character as this is not valid in an event value.
   *
   * @param function a generator for the “event” value of the event
   * @return this
   * @throws Exception any thrown by {@code function}
   */
  Event<T> event(Function<? super T, String> function) throws Exception;

  /**
   * Specify the event type for the server sent event.
   * <p>
   * The value must not contain a {@code '\n'} character as this is not valid in an event value.
   *
   * @param event the event type
   * @return this
   */
  Event<T> event(String event);

  /**
   * Sets the “data” value of the event to the return value of the given function.
   * <p>
   * The function receives the {@link #getItem() item} and is executed immediately.
   *
   * @param function a generator for the “data” value of the event
   * @return this
   * @throws Exception any thrown by {@code function}
   */
  Event<T> data(Function<? super T, String> function) throws Exception;

  /**
   * Specify the event data for the server sent event.
   *
   * @param data the event data
   * @return this
   */
  Event<T> data(String data);

  /**
   * Specify a comment to include as part of this event.
   *
   * @param comment the comment data
   * @return this
   * @since 1.5
   */
  Event<T> comment(String comment);
}
