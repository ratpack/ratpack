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

package ratpack.sse.client.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import ratpack.func.Action;
import ratpack.http.Headers;
import ratpack.http.MutableHeaders;
import ratpack.http.Response;
import ratpack.http.Status;
import ratpack.http.client.StreamedResponse;
import ratpack.sse.ServerSentEvent;
import ratpack.sse.client.ServerSentEventResponse;
import ratpack.sse.internal.ServerSentEventDecodingPublisher;
import ratpack.stream.Streams;
import ratpack.stream.TransformablePublisher;

public class DefaultServerSentEventResponse implements ServerSentEventResponse {

  private final StreamedResponse delegate;
  private final ByteBufAllocator allocator;

  public DefaultServerSentEventResponse(StreamedResponse delegate, ByteBufAllocator allocator) {
    this.delegate = delegate;
    this.allocator = allocator;
  }

  @Override
  public boolean isEventStream() {
    if (getStatus().equals(Status.NO_CONTENT)) {
      return true;
    } else {
      String contentType = getHeaders().get(HttpHeaderNames.CONTENT_TYPE);
      return contentType != null && contentType.startsWith(HttpHeaderValues.TEXT_EVENT_STREAM.toString());
    }
  }

  @Override
  public TransformablePublisher<ServerSentEvent> getEvents() {
    if (!isEventStream()) {
      throw new IllegalStateException("Response is not an event stream; has content type '" + getHeaders().get(HttpHeaderNames.CONTENT_TYPE) + "' and status " + getStatus());
    }

    if (getStatus().equals(Status.NO_CONTENT)) {
      return Streams.empty();
    } else {
      return new ServerSentEventDecodingPublisher(getBody(), allocator)
        .map(e -> e.touch("emit downstream"));
    }
  }

  @Override
  public Status getStatus() {
    return delegate.getStatus();
  }

  @Override
  public int getStatusCode() {
    return delegate.getStatusCode();
  }

  @Override
  public Headers getHeaders() {
    return delegate.getHeaders();
  }

  @Override
  public TransformablePublisher<ByteBuf> getBody() {
    return delegate.getBody();
  }

  @Override
  public void forwardTo(Response response) {
    delegate.forwardTo(response);
  }

  @Override
  public void forwardTo(Response response, Action<? super MutableHeaders> headerMutator) {
    delegate.forwardTo(response, headerMutator);
  }
}
