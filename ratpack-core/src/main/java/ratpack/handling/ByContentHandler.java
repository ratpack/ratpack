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

package ratpack.handling;

/**
 * A buildable strategy for responding based on the HTTP "Accepts" request header.
 * <p>
 * A by-accepts-responder is created by {@link Context#getByContent()}.
 * It is used to respond differently based on what content the client is willing to accept.
 * This is useful when a given handler can provide content of more than one type (i.e. content negotiation).
 * <p>
 * The response to use will be selected based on parsing the "Accepts" header, respecting quality weighting and wildcard matching.
 * The order that types are added to the responder is significant for wildcard matching.
 * The earliest registered type that matches the wildcard will be used.
 * <p>
 * <pre class="tested">
 * import ratpack.handling.*;
 *
 * class MyHandler implements Handler {
 *   public void handle(final Context context) {
 *     // Do processing common to all methods …
 *
 *     context.respond(context.getByContent().
 *       json(new Runnable() {
 *         public void run() {
 *           // JSON responding logic
 *         }
 *       }).
 *       html(new Runnable() {
 *         public void run() {
 *           // HTML handling logic
 *         }
 *       })
 *     );
 *   }
 * }
 * </pre>
 * <p>
 * If you are using Groovy, you can use closures as the definitions…
 * <pre class="tested">
 * import ratpack.groovy.handling.*
 *
 * class MyHandler extends GroovyHandler {
 *   void handle(GroovyContext context) {
 *     context.byContent {
 *       json {
 *         // JSON handling logic
 *       }
 *       html {
 *         // HTML handling logic
 *       }
 *     }
 *   }
 * }
 * </pre>
 * If there is no type registered before {@link #handle(Context)} is called, or the client does not accept any
 * of the given types, a {@code 406} will be issued to the {@link Context#clientError(int)}
 * that the responder is associated with.
 * <p>
 * Only the last added runnable for a type will be used.
 * Adding a subsequent runnable for the same type will replace the previous.
 */
public interface ByContentHandler extends Handler {

  /**
   * Register how to respond with the given mime type.
   *
   * @param mimeType The mime type to register for
   * @param runnable The action to take if the client wants the given type
   * @return this
   */
  ByContentHandler type(String mimeType, Runnable runnable);

  /**
   * Convenience method to respond with "text/plain" mime type.
   *
   * @param runnable the action to take if the client wants plain text
   * @return this
   */
  ByContentHandler plainText(Runnable runnable);

  /**
   * Convenience method to respond with "text/html" mime type.
   *
   * @param runnable the action to take if the client wants html
   * @return this
   */
  ByContentHandler html(Runnable runnable);

  /**
   * Convenience method to respond with "application/json" mime type.
   *
   * @param runnable the action to take if the client wants json
   * @return this
   */
  ByContentHandler json(Runnable runnable);

  /**
   * Convenience method to respond with "application/xml" mime type.
   *
   * @param runnable the action to take if the client wants xml
   * @return this
   */
  ByContentHandler xml(Runnable runnable);

}
