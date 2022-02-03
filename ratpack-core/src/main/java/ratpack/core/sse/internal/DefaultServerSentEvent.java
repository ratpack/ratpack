/*
 * Copyright 2021 the original author or authors.
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

package ratpack.core.sse.internal;

import ratpack.func.Nullable;
import ratpack.core.sse.ServerSentEvent;
import ratpack.core.sse.ServerSentEventBuilder;

public class DefaultServerSentEvent implements ServerSentEvent, ServerSentEventBuilder {

  @Nullable
  private String id;
  @Nullable
  private String event;
  @Nullable
  private String data;
  @Nullable
  private String comment;

  @Nullable
  @Override
  public String getId() {
    return id;
  }

  @Nullable
  @Override
  public String getEvent() {
    return event;
  }

  @Nullable
  @Override
  public String getData() {
    return data;
  }

  @Nullable
  @Override
  public String getComment() {
    return comment;
  }

  @Override
  public ServerSentEventBuilder id(@Nullable String id) {
    if (id != null && id.contains("\n")) {
      throw new IllegalArgumentException("id must not contain \\n - '" + id + "'");
    }
    this.id = id;
    return this;
  }

  @Override
  public ServerSentEventBuilder event(@Nullable String event) {
    if (event != null && event.contains("\n")) {
      throw new IllegalArgumentException("event must not contain \\n - '" + event + "'");
    }

    this.event = event;
    return this;
  }

  @Override
  public ServerSentEventBuilder data(@Nullable String data) {
    this.data = data;
    return this;
  }

  @Override
  public ServerSentEventBuilder comment(@Nullable String comment) {
    this.comment = comment;
    return this;
  }

  @Override
  public ServerSentEvent build() {
    return this;
  }
}
