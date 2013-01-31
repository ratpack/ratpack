/*
 * Copyright 2012 the original author or authors.
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

package org.ratpackframework.routing.internal;

import org.ratpackframework.Response;
import org.ratpackframework.Routing;
import org.ratpackframework.routing.ResponseFactory;
import org.ratpackframework.routing.RoutedRequest;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServer;

import java.util.List;

public class RoutingBuilder implements Routing {

  private final Vertx vertx;
  private final HttpServer httpServer;
  private final List<Handler<RoutedRequest>> routers;
  private final ResponseFactory responseFactory;

  RoutingBuilder(Vertx vertx, HttpServer httpServer, List<Handler<RoutedRequest>> routers, ResponseFactory responseFactory) {
    this.vertx = vertx;
    this.httpServer = httpServer;
    this.routers = routers;
    this.responseFactory = responseFactory;
  }

  @Override
  public Routing getRouting() {
    return this;
  }

  @Override
  public Vertx getVertx() {
    return vertx;
  }

  @Override
  public HttpServer getHttpServer() {
    return httpServer;
  }

  @Override
  public void register(String method, String path, Handler<Response> handler) {
    routers.add(new PathRouter(path, method, responseFactory, new ErrorHandlingResponseHandler(handler)));
  }

  @Override
  public void all(String path, Handler<Response> handler) {
    register(Routing.ALL_METHODS, path, handler);
  }

  @Override
  public void get(String path, Handler<Response> handler) {
    register("get", path, handler);
  }

  @Override
  public void post(String path, Handler<Response> handler) {
    register("post", path, handler);
  }

  @Override
  public void put(String path, Handler<Response> handler) {
    register("put", path, handler);
  }

  @Override
  public void delete(String path, Handler<Response> handler) {
    register("delete", path, handler);
  }

}
