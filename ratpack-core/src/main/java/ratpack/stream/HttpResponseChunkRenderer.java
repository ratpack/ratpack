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

import ratpack.render.Renderer;

/**
 * A renderer for streaming HTTP chunks for HTTP chunked transfer-encoding.
 * <p>
 * An implementation of this is <b>always</b> provided by Ratpack core.
 * <p>
 * Example usage:
 * <pre class="tested">
 * import ratpack.handling.Handler;
 * import ratpack.handling.Context;
 * import ratpack.stream.HttpResponseChunk;
 * import org.reactivestreams.Publisher;
 * import static ratpack.stream.HttpResponseChunks.httpResponseChunks
 *
 * public class HttpChunkRenderingHandler implements Handler {
 *
 *   private Publisher<HttpResponseChunk> chunkStream;
 *
 *   public HttpChunkRenderingHandler(Publisher<HttpResponseChunk> chunkStream) {
 *     this.chunkStream = chunkStream;
 *   }
 *
 *   public void handle(Context context) {
 *     context.render(httpResponseChunks(chunkStream));
 *   }
 * }
 * </pre>
 *
 * @see ratpack.stream.HttpResponseChunks
 * @see <a href="http://en.wikipedia.org/wiki/Chunked_transfer_encoding" target="_blank">Wikipedia - Chunked transfer encoding</a>
 */
public interface HttpResponseChunkRenderer extends Renderer<HttpResponseChunks> {
}
