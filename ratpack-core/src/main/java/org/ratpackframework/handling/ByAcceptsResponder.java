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

package org.ratpackframework.handling;

import org.ratpackframework.util.Buildable;

/**
 * A buildable strategy for responding based on the HTTP "Accepts" request header.
 * <p>
 * A by-accepts-responder is created by {@link Context#getAccepts()}.
 * It is used to respond differently based on what content the client is willing to accept.
 * This is useful when a given handler can provide content of more than one type (i.e. content negotiation).
 * <p>
 * The response to use will be selected based on parsing the "Accepts" header, respecting quality weighting and wildcard matching.
 * The order that types are added to the responder is significant for wildcard matching.
 * The earliest registered type that matches the wildcard will be used.
 * <p>
 * <pre class="tested">
 * import org.ratpackframework.handling.*;
 *
 * class MyHandler implements Handler {
 *   public void handle(final Context exchange) {
 *     // Do processing common to all methods …
 *
 *     exchange.getAccepts().
 *       type("application/json", new Runnable() {
 *         public void run() {
 *           // JSON responding logic
 *         }
 *       }).
 *       type("text/html", new Runnable() {
 *         public void run() {
 *           // HTML handling logic
 *         }
 *       }).
 *       build(); // finalize
 *   }
 * }
 * </pre>
 * <p>
 * If you are using Groovy, you can use closures as the definitions (because closures implement {@link Runnable}),
 * along with {@code Buildable.with}…
 * <pre class="tested">
 * import org.ratpackframework.handling.*
 * import static org.ratpackframework.groovy.Util.with
 *
 * class MyHandler implements Handler {
 *   void handle(Context exchange) {
 *     with(exchange.accepts) {
 *       type("application/json") {
 *         // JSON handling logic
 *       }
 *       type("text/html") {
 *         // HTML handling logic
 *       }
 *     }
 *   }
 * }
 * </pre>
 * <p>
 * You <b>must</b> call the {@link #build()} method to finalise the responder. Otherwise, nothing will happen.
 * <p>
 * If there is no action registered with the responder before {@link #build()} is called, or the client does not accept any
 * of the given types, a {@code 406} will be issued to the {@link Context#clientError(int)}
 * that the responder is associated with.
 * <p>
 * Only the last added runnable for a type will be used.
 * Adding a subsequent runnable for the same type will replace the previous.
 */
public interface ByAcceptsResponder extends Buildable {

  /**
   * Register how to respond with the given mime type.
   *
   * @param mimeType The mime type to register for.
   * @param runnable The code that responds with the given mime type (should terminate the response)
   * @return this
   */
  ByAcceptsResponder type(String mimeType, Runnable runnable);

  /**
   * Finalizes this responder, invoking the appropriate registered runnable.
   */
  void build();

}
