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

package ratpack.stream;

import ratpack.api.Nullable;

/**
 * Represents an event emitted by a publisher.
 *
 * @param <T> the type of data item
 * @see Streams#wiretap(org.reactivestreams.Publisher, ratpack.func.Action)
 */
public interface StreamEvent<T> {

  /**
   * The opaque id of the subscription that this event relates to.
   * <p>
   * Events are emitted for each subscription to a publisher.
   * This value can be used for differentiating events from different subscriptions.
   * <p>
   * The ids are only unique for a context. That is, they are not globally unique.
   * More precise semantics of ids should be specified by methods that emit events.
   *
   * @return an opaque id of the subscription this event belongs to
   */
  int getSubscriptionId();

  /**
   * Whether or not this event represents the completion of the stream.
   *
   * @return whether or not this event represents the completion of the stream
   */
  boolean isComplete();

  /**
   * Whether or not this event represents an error.
   * <p>
   * If this method returns {@code true}, {@link #getThrowable()} will return the corresponding exception.
   * If this method returns {@code false}, {@link #getThrowable()} will return {@code null}.
   *
   * @return whether or not this event represents an error
   */
  boolean isError();

  /**
   * Whether or not this event represents an emission of data.
   * <p>
   * If this method returns {@code true}, {@link #getItem()} will return the corresponding data.
   * If this method returns {@code false}, {@link #getItem()} will return {@code null}.
   *
   * @return whether or not this event represents an error
   */
  boolean isData();

  /**
   * Whether or not this event represents cancellation of the stream.
   *
   * @return whether or not this event represents cancellation of the stream
   */
  boolean isCancel();

  /**
   * Whether or not this event represents a request for more data.
   * <p>
   * If this method returns {@code true}, {@link #getRequestAmount()} will return the amount requested.
   * If this method returns {@code false}, {@link #getRequestAmount()} will return {@code 0}.
   *
   * @return whether or not this event represents an error
   */
  boolean isRequest();

  /**
   * The request amount, if this event represents a request.
   * <p>
   * If {@link #isRequest()} returns {@code true}, this method will return the corresponding request amount.
   * If {@link #isRequest()} returns {@code false}, this method will return {@code 0}.
   *
   * @return the request amount if this event represents a request, else {@code 0}
   */
  long getRequestAmount();

  /**
   * The error, if this event represents an error.
   * <p>
   * If {@link #isError()} returns {@code true}, this method will return the corresponding exception.
   * If {@link #isError()} returns {@code false}, this method will return null.
   *
   * @return the error if this event represents an error, else {@code null}
   */
  @Nullable
  Throwable getThrowable();

  /**
   * The data, if this event represents an emission of data.
   * <p>
   * If {@link #isData()} returns {@code true}, this method will return the corresponding data item.
   * If {@link #isData()} returns {@code false}, this method will return null.
   *
   * @return the data if this event represents data, else {@code null}
   */
  @Nullable
  T getItem();

}
