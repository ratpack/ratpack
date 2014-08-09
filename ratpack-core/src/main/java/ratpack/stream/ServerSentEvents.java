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

import org.reactivestreams.Publisher;

/**
 * A factory for creating "wrapped" {@link ratpack.stream.ServerSentEvent} publishers.
 * <p>
 * Wrapping the <code>Publisher<ServerSentEvent></code> allows a {@link ratpack.render.Renderer} to be used
 * which we otherwise couldn't do as Renderers don't support matching based on generic types.
 * <p>
 * See {@link ratpack.stream.ServerSentEventsRenderer} to see an example of this in action.
 */
public abstract class ServerSentEvents {

  public static ServerSentEvents serverSentEvents(final Publisher<ServerSentEvent> publisher) {
    return new ServerSentEvents() {
      @Override
      public Publisher<ServerSentEvent> getPublisher() {
        return publisher;
      }
    };
  }

  private ServerSentEvents() {}

  public abstract Publisher<ServerSentEvent> getPublisher();
}
