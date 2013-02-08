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

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import org.ratpackframework.Response;
import org.ratpackframework.Routing;
import org.ratpackframework.handler.Handler;
import org.ratpackframework.routing.ResponseFactory;
import org.ratpackframework.routing.RoutedRequest;

import java.util.List;

public class RoutingBuilder implements Routing {

  private final List<Handler<RoutedRequest>> routers;
  private final ResponseFactory responseFactory;
  private final Injector injector;

  public RoutingBuilder(Injector injector, List<Handler<RoutedRequest>> routers, ResponseFactory responseFactory) {
    this.injector = injector;
    this.routers = routers;
    this.responseFactory = responseFactory;
  }

  @Override
  public <T> T service(Class<T> type) {
    return injector.getInstance(type);
  }

  @Override
  public void register(String method, String path, Handler<Response> handler) {
    routers.add(new PathRouter(path, method, responseFactory, new ErrorHandlingResponseHandler(handler)));
  }

  @Override
  public void register(String method, String path, final Class<? extends Handler<Response>> handlerType) {
    Handler<Response> instance = injector.createChildInjector(new AbstractModule() {
      @Override
      protected void configure() {
        binder().newPrivateBinder().bind(new TypeLiteral<Handler<Response>>() {
        }).to(handlerType);
      }
    }).getInstance(handlerType);

    register(method, path, instance);
  }

  @Override
  public void all(String path, Handler<Response> handler) {
    register(Routing.ALL_METHODS, path, handler);
  }

  @Override
  public void all(String path, Class<? extends Handler<Response>> handlerType) {
    register(Routing.ALL_METHODS, path, handlerType);
  }

  @Override
  public void get(String path, Handler<Response> handler) {
    register("get", path, handler);
  }

  @Override
  public void get(String path, Class<? extends Handler<Response>> handlerType) {
    register("get", path, handlerType);
  }

  @Override
  public void post(String path, Handler<Response> handler) {
    register("post", path, handler);
  }

  @Override
  public void post(String path, Class<? extends Handler<Response>> handlerType) {
    register("post", path, handlerType);
  }

  @Override
  public void put(String path, Handler<Response> handler) {
    register("put", path, handler);
  }

  @Override
  public void put(String path, Class<? extends Handler<Response>> handlerType) {
    register("put", path, handlerType);
  }

  @Override
  public void delete(String path, Handler<Response> handler) {
    register("delete", path, handler);
  }

  @Override
  public void delete(String path, Class<? extends Handler<Response>> handlerType) {
    register("delete", path, handlerType);
  }
}
