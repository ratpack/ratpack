/*
 * Copyright 2013 the original author or authors.
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

package ratpack.file;

import ratpack.render.Renderer;

import java.nio.file.Path;

/**
 * A renderer for static files.
 * <p>
 * An implementation of this is <b>always</b> provided by Ratpack core.
 * <p>
 * Example usage:
 * <pre class="tested">
 * import ratpack.handling.Handler;
 * import ratpack.handling.Context;
 * import java.nio.file.Path;
 *
 * public class FileRenderingHandler implements Handler {
 *   public void handle(Context context) {
 *     Path file = context.file("some/file/to/serve.txt");
 *     context.render(file);
 *   }
 * }
 * </pre>
 * <p>
 * Use of this renderer should be preferred over {@link ratpack.http.Response#sendFile} as it handles
 * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html">HTTP Caching</a> by way of the {@code If-Modified-Since} header,
 * based on timestamps reported by the filesystem.
 * <p>
 * If the {@link ratpack.http.Response#contentType(String)} has not been set prior to calling this method,
 * it will be guessed by retrieving the {@link MimeTypes} from the context and using it to calculate the type
 * based on the name of the file.
 */
public interface FileRenderer extends Renderer<Path> {}
