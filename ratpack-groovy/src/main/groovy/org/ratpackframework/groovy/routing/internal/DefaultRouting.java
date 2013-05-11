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

  @Override
  public void route(Closure<?> handler) {
    route(ClosureHandlers.handler(handler));
  }

  @Override
  public void routes(Closure<?> routing) {
    route(new RoutingHandler(action(routing)));
  }

  @Override
  public void path(String path, Closure<?> routing) {
    route(Handlers.path(path, new RoutingHandler(action(routing))));
  }

  @Override
  public void all(String path, Closure<?> handler) {
    route(ClosureHandlers.handler(path, handler));
  }

  @Override
  public void handler(String path, List<String> methods, Closure<?> handler) {
    route(ClosureHandlers.handler(path, methods, handler));
  }

  @Override
  public void get(String path, Closure<?> handler) {
    route(ClosureHandlers.get(path, handler));
  }

  @Override
  public void get(Closure<?> handler) {
    get("", handler);
  }

  @Override
  public void post(String path, Closure<?> handler) {
    route(ClosureHandlers.post(path, handler));
  }

  @Override
  public void post(Closure<?> handler) {
    post("", handler);
  }

  @Override
  public void assets(String path, String... indexFiles) {
    route(Handlers.assets(path, indexFiles));
  }

  @Override
  public void fsContext(String path, Closure<?> routing) {
    route(Handlers.fsContext(path, new RoutingHandler(action(routing))));
  }

  @Override
  public Exchange getExchange() {
    return exchange;
  }

  @Override
  public void route(Handler handler) {
    handlers.add(handler);
  }
}
