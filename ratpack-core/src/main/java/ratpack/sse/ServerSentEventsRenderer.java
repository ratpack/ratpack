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

import ratpack.render.Renderer;

/**
 * A renderer for streaming Server Sent Events.
 * <p>
 * An implementation of this is <b>always</b> provided by Ratpack core.
 * <p>
 * Example usage:
 * <pre class="tested">
 * import ratpack.handling.Handler;
 * import ratpack.handling.Context;
 * import ratpack.sse.ServerSentEvent;
 * import org.reactivestreams.Publisher;
 * import static ratpack.sse.ServerSentEvents.serverSentEvents
 *
 * public class ServerSentEventRenderingHandler implements Handler {
 *
 *   private Publisher&lt;ServerSentEvent&gt; chunkStream;
 *
 *   public ServerSentEventRenderingHandler(Publisher&lt;ServerSentEvent&gt; chunkStream) {
 *     this.chunkStream = chunkStream;
 *   }
 *
 *   public void handle(Context context) {
 *     context.render(serverSentEvents(chunkStream));
 *   }
 * }
 * </pre>
 *
 * @see ServerSentEvents
 * @see <a href="http://en.wikipedia.org/wiki/Server-sent_events" target="_blank">Wikipedia - Using server-sent events</a>
 * @see <a href="https://developer.mozilla.org/en-US/docs/Server-sent_events/Using_server-sent_events" target="_blank">MDN - Using server-sent events</a>
 */
public interface ServerSentEventsRenderer extends Renderer<ServerSentEvents> {
}
