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

import org.ratpackframework.api.NonBlocking;

/**
 * A handler participates in the processing of a request/response pair (i.e. an {@link Exchange}).
 * <p>
 * Handlers are the heart and soul of Ratpack applications. The entire request processing logic is composed of different handlers.
 * A single handler is given to the server builder ({@link org.ratpackframework.bootstrap.RatpackServerBuilder}) when bootstrapping
 * an application that will receive every single request. This handler is typically some type of composite of different types of handlers.
 * <h3>Non blocking/Asynchronous</h3>
 * <p>
 * Handlers are fundamentally asynchronous. This means that there is no guarantee that the handler is “finished” when its {@link #handle(Exchange)}
 * method returns. This means that handlers <b>must</b> ensure to do <i>something</i> with the response. Where <i>something</i> is either send
 * a response or delegate to another handler.
 * <h3>Types of handlers</h3>
 * <p>
 * Handlers do not necessarily generate a response to the request, though they can.
 * They may delegate to, or cooperate with other handlers.
 * <p>
 * Handlers can generally speaking do the four following kinds of things:
 * <p>
 * <ol>
 * <li>Send a response back to the client, terminating processing</li>
 * <li>Pass control to the next handler in the pipeline via {@link org.ratpackframework.handling.Exchange#next()}</li>
 * <li>Insert handlers into the pipeline via {@link Exchange#insert(java.util.List)} (or related methods) before passing on control</li>
 * <li>Forward the exchange to another handler (that it has a reference to) by directly calling its {@link #handle(Exchange)} method</li>
 * </ol>
 * <p>
 * A handler can either generate a response, decorate the response (e.g. add a response header) or direct the exchange to different handlers.
 * They are effectively a function that operates on the HTTP exchange.
 * <p>
 * <h3>Examples</h3>
 * Where there is no strict taxonomy of handlers, the following are indicative examples of common functions.
 * <p>
 * <h4>Simple Responder</h4>
 * <p>
 * A simple "responder" type handler may just unconditionally return a response to the client.
 * <pre>
 * class SimpleHandler implements Handler {
 *   void handle(Exchange exchange) {
 *     exchange.getResponse().send("Hello World!");
 *   }
 * }
 * </pre>
 * <h4>Conditional Responder</h4>
 * <p>
 * Or a handler may conditionally respond…
 * <pre>
 * class ConditionalHandler implements Handler {
 *   void handle(Exchange exchange) {
 *     if (exchange.getRequest().getPath().equals("foo")) {
 *       exchange.getResponse().send("Hello World!");
 *     } else {
 *       exchange.next();
 *     }
 *   }
 * }
 * </pre>
 * <h4>Router</h4>
 * <p>
 * A handler does not need to participate in the response, but can instead "route" the exchange to different handlers.
 * <pre>
 * class RoutingHandler implements Handler {
 *   private final List&lt;Handler&gt; fooHandlers;
 *
 *   public RoutingHandler(List&lt;Handler&gt; fooHandlers) {
 *     this.fooHandlers = fooHandlers;
 *   }
 *
 *   void handle(Exchange exchange) {
 *     if (exchange.getRequest().getPath().startsWith("foo/")) {
 *       exchange.insert(fooHandlers);
 *     } else {
 *       exchange.next();
 *     }
 *   }
 * }
 * </pre>
 * <p>
 * <i>This content is still work in progress.</i>
 *
 * @see Handlers
 * @see Chain
 */
public interface Handler {

  /**
   * Handles the exchange.
   *
   * @param exchange The exchange to handle
   * @see Handler
   */
  @NonBlocking
  void handle(Exchange exchange);

}
