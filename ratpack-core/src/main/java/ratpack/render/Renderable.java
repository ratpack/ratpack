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

package ratpack.render;

import ratpack.handling.Context;

/**
 * A renderable object, that can be given to {@link Context#render(Object)}.
 * <p>
 * A {@link Renderer} for this type is provided by Ratpack core, that simply delegates to {@link #render(Context)}.
 * An alternative to providing a {@link Renderer} implementation for a type is to make the type implement this interface.
 * <pre class="java">{@code
 * import ratpack.handling.Context;
 * import ratpack.render.Renderable;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *   static class Thing implements Renderable {
 *     public void render(Context context) {
 *       context.render("thing!");
 *     }
 *   }
 *
 *   static void main(String... args) throws Exception {
 *     EmbeddedApp.fromHandler(ctx ->
 *       ctx.render(new Thing())
 *     ).test(httpClient ->
 *       assertEquals("thing!", httpClient.getText())
 *     );
 *   }
 * }
 * }</pre>
 * <p>
 * An alternative to making a type implement this interface, is implementing a {@link Renderer} for it.
 */
public interface Renderable {

  /**
   * Render this object to the response.
   *
   * @param context the request handling context
   * @throws Exception any
   */
  void render(Context context) throws Exception;

}
