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

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.ratpackframework.app.Endpoint;
import org.ratpackframework.app.Response;
import org.ratpackframework.app.internal.binding.PathBinding;
import org.ratpackframework.handler.Handler;
import org.ratpackframework.http.HttpExchange;
import org.ratpackframework.routing.Routed;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RoutingAdapter implements Handler<Routed<HttpExchange>> {

  private final PathBinding pathBinding;
  private final ResponseFactory responseFactory;
  private final Endpoint endpoint;
  private final List<String> methods;

  public RoutingAdapter(PathBinding pathBinding, ResponseFactory responseFactory, Endpoint endpoint, String... methods) {
    this.pathBinding = pathBinding;
    this.responseFactory = responseFactory;
    this.endpoint = endpoint;
    this.methods = Arrays.asList(methods);
  }

  @Override
  public void handle(Routed<HttpExchange> routedHttpExchange) {
    HttpExchange exchange = routedHttpExchange.get();
    HttpRequest request = exchange.getRequest();
    if (methods.isEmpty() || methods.contains(request.getMethod().getName())) {
      String path = exchange.getPath();

      Map<String, String> params = pathBinding.map(path);
      if (params != null) {
        Response response = responseFactory.create(routedHttpExchange, params);
        endpoint.respond(response.getRequest(), response);
        return;
      }
    }

    routedHttpExchange.next();
  }

}
