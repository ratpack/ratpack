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

package ratpack.sse.internal;

import org.reactivestreams.Publisher;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.stream.Streams;
import ratpack.stream.TransformablePublisher;

@SuppressWarnings("deprecation")
public class DefaultEvent<T> implements ratpack.sse.Event<T> {

  private final T item;

  private String id;
  private String event;
  private String data;
  private String comment;

  public DefaultEvent(T item) {
    this.item = item;
  }

  public static <T> TransformablePublisher<ratpack.sse.Event<T>> toEvents(Publisher<? extends T> publisher, Action<? super ratpack.sse.Event<T>> action) {
    return Streams.map(publisher, item -> {
      ratpack.sse.Event<T> event = action.with(new DefaultEvent<>(item));
      if (event.getData() == null && event.getId() == null && event.getEvent() == null && event.getComment() == null) {
        throw new IllegalArgumentException("You must supply at least one of data, event, id or comment");
      }
      return event;
    });
  }

  @Override
  public T getItem() {
    return item;
  }

  @Override
  public ratpack.sse.Event<T> id(Function<? super T, String> id) throws Exception {
    id(id.apply(item));
    return this;
  }

  @Override
  public ratpack.sse.Event<T> id(String id) {
    if (id.contains("\n")) {
      throw new IllegalArgumentException("id must not contain \\n - '" + id + "'");
    }
    this.id = id;
    return this;
  }

  @Override
  public ratpack.sse.Event<T> event(Function<? super T, String> id) throws Exception {
    event(id.apply(item));
    return this;
  }

  @Override
  public ratpack.sse.Event<T> event(String event) {
    if (event.contains("\n")) {
      throw new IllegalArgumentException("event must not contain \\n - '" + event + "'");
    }
    this.event = event;
    return this;
  }

  @Override
  public ratpack.sse.Event<T> data(Function<? super T, String> id) throws Exception {
    data(id.apply(item));
    return this;
  }

  @Override
  public ratpack.sse.Event<T> data(String data) {
    this.data = data;
    return this;
  }

  @Override
  public ratpack.sse.Event<T> comment(String comment) {
    this.comment = comment;
    return this;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getEvent() {
    return event;
  }

  @Override
  public String getData() {
    return data;
  }

  @Override
  public String getComment() {
    return comment;
  }

  @Override
  public String toString() {
    return "Event{id='" + id + '\'' + ", event='" + event + '\'' + ", data='" + data + '\'' + '}';
  }
}