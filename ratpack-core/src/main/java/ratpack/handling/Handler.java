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

import ratpack.api.NonBlocking;

/**
 * A handler participates in the processing of a request/response pair, operating in a {@link Context}.
 * <p>
 * Handlers are the heart and soul of Ratpack applications.
 * The entire request processing logic is defined by chains of handlers.
 * The root handler (defined as part of the {@link ratpack.launch.LaunchConfig}) responds to all requests.
 * A handler can choose to send a response, or delegate to another handler in a few different ways.
 * </p>
 * <h3>Non blocking/Asynchronous</h3>
 * <p>
 * Handlers are asynchronous, in that they are free to pass control to a different thread.
 * This means that there is no guarantee that the handler is “finished” when its {@link #handle(Context)} method returns.
 * An implication is that handlers <b>must</b> ensure to do <i>something</i> with the response.
 * Where <i>something</i> is either send a response or delegate to another handler.
 * <h3>Handler chains</h3>
 * <p>
 * Handlers are always implicitly connected in a chain like structure.
 * The {@link Context} that the handler operates on provides the {@link Context#next()} method that passes control to the next handler in the chain.
 * The last handler in the chain is always the “404” handler in that it returns an empty body 404 response to the client.
 * <p>
 * Handler chains are built using the {@link Chain} type, typically during application bootstrapping.
 * <h3>Types of handlers</h3>
 * <p>
 * Handlers do not necessarily generate a response to the request, though they can.
 * They may delegate to, or cooperate with other handlers.
 * <p>
 * Handlers can generally speaking do the four following kinds of things:
 * <p>
 * <ol>
 * <li>Send a response back to the client, terminating processing</li>
 * <li>Pass control to the next handler in the pipeline via {@link Context#next()}</li>
 * <li>Insert handlers into the pipeline via {@link Context#insert(java.util.List)} (or related methods) before passing on control</li>
 * <li>Forward the exchange to another handler (that it has a reference to) by directly calling its {@link #handle(Context)} method</li>
 * </ol>
 * <p>
 * A handler can either generate a response, decorate the response (e.g. add a response header) or direct the exchange to different handlers.
 * They are effectively a function that operates on the HTTP exchange.
 * <h3>Examples</h3>
 * While there is no strict taxonomy of handlers, the following are indicative examples of common functions.
 * <p>
 * <pre class="tested">
 * import ratpack.handling.*;
 *
 *
 * // A responder may just return a response to the client…
 *
 * class SimpleHandler implements Handler {
 *   void handle(Context exchange) {
 *     exchange.getResponse().send("Hello World!");
 *   }
 * }
 *
 * // A responder may add a response header, but not the body…
 *
 * class DecoratingHandler implements Handler {
 *   void handle(Context exchange) {
 *     exchange.getResponse().getHeaders().set("Cache-Control", "no-cache");
 *     exchange.next();
 *   }
 * }
 *
 * // Or a handler may conditionally respond…
 *
 * class ConditionalHandler implements Handler {
 *   void handle(Context exchange) {
 *     if (exchange.getRequest().getPath().equals("foo")) {
 *       exchange.getResponse().send("Hello World!");
 *     } else {
 *       exchange.next();
 *     }
 *   }
 * }
 *
 * // A handler does not need to participate in the response, but can instead "route" the exchange to different handlers…
 *
 * class RoutingHandler implements Handler {
 *   private final List&lt;Handler&gt; fooHandlers;
 *
 *   public RoutingHandler(List&lt;Handler&gt; fooHandlers) {
 *     this.fooHandlers = fooHandlers;
 *   }
 *
 *   void handle(Context exchange) {
 *     if (exchange.getRequest().getPath().startsWith("foo/")) {
 *       exchange.insert(fooHandlers);
 *     } else {
 *       exchange.next();
 *     }
 *   }
 * }
 *
 * // It can sometimes be appropriate to directly delegate to a handler, instead of using exchange.insert() …
 *
 * class FilteringHandler implements Handler {
 *   private final Handler nestedHandler;
 *
 *   public FilteringHandler(Handler nestedHandler) {
 *     this.nestedHandler = nestedHandler;
 *   }
 *
 *   void handle(Context exchange) {
 *     if (exchange.getRequest().getPath().startsWith("foo/")) {
 *       nestedHandler.handle(exchange);
 *     } else {
 *       exchange.next();
 *     }
 *   }
 * }
 * </pre>
 *
 * @see Handlers
 * @see Chain
 * @see ratpack.registry.Registry
 */
public interface Handler {

  /**
   * Handles the context.
   *
   * @param context The context to handle
   */
  @NonBlocking
  void handle(Context context);

}
