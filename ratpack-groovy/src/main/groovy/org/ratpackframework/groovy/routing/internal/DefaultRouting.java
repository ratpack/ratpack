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

package org.ratpackframework.groovy.routing.internal;

import groovy.lang.Closure;
import org.ratpackframework.groovy.ClosureHandlers;
import org.ratpackframework.groovy.routing.Routing;
import org.ratpackframework.routing.Exchange;
import org.ratpackframework.routing.Handler;
import org.ratpackframework.routing.Handlers;

import java.util.List;

import static org.ratpackframework.groovy.Closures.action;

public class DefaultRouting implements Routing {

  private final List<Handler> handlers;
  private final Exchange exchange;

  DefaultRouting(Exchange exchange, List<Handler> handlers) {
    this.exchange = exchange;
    this.handlers = handlers;
  }

  public void route(Closure<?> handler) {
    route(ClosureHandlers.handler(handler));
  }

  public void routes(Closure<?> routing) {
    route(routing(routing));
  }

  public void path(String path, Closure<?> routing) {
    route(Handlers.path(path, routing(routing)));
  }

  public void all(String path, Closure<?> handler) {
    route(ClosureHandlers.handler(path, handler));
  }

  public void handler(String path, List<String> methods, Closure<?> handler) {
    route(ClosureHandlers.handler(path, methods, handler));
  }

  public void get(String path, Closure<?> handler) {
    route(ClosureHandlers.get(path, handler));
  }

  public void get(Closure<?> handler) {
    get("", handler);
  }

  public void post(String path, Closure<?> handler) {
    route(ClosureHandlers.post(path, handler));
  }

  public void post(Closure<?> handler) {
    post("", handler);
  }

  public void assets(String path, String... indexFiles) {
    route(Handlers.assets(path, indexFiles));
  }

  public void context(Object object, Closure<?> routing) {
    route(Handlers.context(object, routing(routing)));
  }

  public void fsContext(String path, Closure<?> routing) {
    route(Handlers.fsContext(path, routing(routing)));
  }

  private Handler routing(Closure<?> routing) {
    return new RoutingHandler(action(routing));
  }

  public Exchange getExchange() {
    return exchange;
  }

  public void route(Handler handler) {
    handlers.add(handler);
  }


}
