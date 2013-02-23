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

package org.ratpackframework.app.internal;

import com.google.inject.Injector;
import com.google.inject.Key;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.ratpackframework.app.Endpoint;
import org.ratpackframework.app.InjectingEndpoint;
import org.ratpackframework.app.Routing;
import org.ratpackframework.app.internal.binding.PathBinding;
import org.ratpackframework.app.internal.binding.PatternPathBinding;
import org.ratpackframework.app.internal.binding.TokenPathBinding;
import org.ratpackframework.handler.Handler;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.routing.Routed;

import java.util.List;

public class DefaultRouting implements Routing {

  private final List<Handler<Routed<HttpExchange>>> routers;
  private final ResponseFactory responseFactory;
  private final Injector injector;
  private final RequestScope requestScope;

  public DefaultRouting(Injector injector, RequestScope requestScope, List<Handler<Routed<HttpExchange>>> routers, ResponseFactory responseFactory) {
    this.injector = injector;
    this.requestScope = requestScope;
    this.routers = routers;
    this.responseFactory = responseFactory;
  }

  @Override
  public Injector getInjector() {
    return injector;
  }

  @Override
  public <T> T service(Class<T> type) {
    return injector.getInstance(type);
  }

  @Override
  public <T> T service(Key<T> key) {
    return injector.getInstance(key);
  }

  @Override
  public Endpoint inject(Class<? extends Endpoint> endpointType) {
    if (Endpoint.class.isAssignableFrom(endpointType)) {
      return new InjectingEndpoint(injector, endpointType);
    } else {
      throw new IllegalArgumentException(endpointType.getName() + " does not implement " + Endpoint.class.getName());
    }
  }

  private void register(String method, PathBinding pathBinding, Endpoint endpoint) {
    Endpoint responseHandler = new ErrorHandlingEndpointDecorator(new RequestScopeEndpointDecorator(requestScope, endpoint));
    String[] methods;
    if (method.equals(ALL_METHODS)) {
      methods = new String[0];
    } else {
      methods = new String[]{method};
    }
    Handler<Routed<HttpExchange>> pathRouter = new RoutingAdapter(pathBinding, responseFactory, responseHandler, methods);
    routers.add(pathRouter);
  }

  @Override
  public void route(String method, String path, Endpoint endpoint) {
    register(method.toUpperCase(), new TokenPathBinding(path), endpoint);
  }

  @Override
  public void routeRe(String method, String pattern, Endpoint endpoint) {
    register(method.toUpperCase(), new PatternPathBinding(pattern), endpoint);
  }

  @Override
  public void all(String path, Endpoint endpoint) {
    route(Routing.ALL_METHODS, path, endpoint);
  }

  @Override
  public void allRe(String path, Endpoint endpoint) {
    route(Routing.ALL_METHODS, path, endpoint);
  }

  @Override
  public void get(String path, Endpoint endpoint) {
    route(HttpMethod.GET.getName(), path, endpoint);
  }

  @Override
  public void getRe(String pattern, Endpoint endpoint) {
    routeRe(HttpMethod.GET.getName(), pattern, endpoint);
  }

  @Override
  public void post(String path, Endpoint endpoint) {
    route(HttpMethod.POST.getName(), path, endpoint);
  }

  @Override
  public void postRe(String pattern, Endpoint endpoint) {
    routeRe(HttpMethod.POST.getName(), pattern, endpoint);
  }

  @Override
  public void put(String path, Endpoint endpoint) {
    route(HttpMethod.PUT.getName(), path, endpoint);
  }

  @Override
  public void putRe(String pattern, Endpoint endpoint) {
    routeRe(HttpMethod.PUT.getName(), pattern, endpoint);
  }

  @Override
  public void delete(String path, Endpoint endpoint) {
    route(HttpMethod.DELETE.getName(), path, endpoint);
  }

  @Override
  public void deleteRe(String pattern, Endpoint endpoint) {
    routeRe(HttpMethod.DELETE.getName(), pattern, endpoint);
  }

}
