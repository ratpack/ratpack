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

package org.ratpackframework.routing

import groovy.transform.CompileStatic
import org.ratpackframework.HandlerClosure
import org.ratpackframework.Request
import org.ratpackframework.Response
import org.ratpackframework.Routing
import org.ratpackframework.responder.internal.ClosureBackedResponderFactory
import org.ratpackframework.routing.internal.PathRouter
import org.ratpackframework.templating.TemplateRenderer

@CompileStatic
class RouterBuilderScript extends Script implements Routing {

  private final List<Router> routers = []

  private final TemplateRenderer templateRenderer

  RouterBuilderScript(List<Router> routers, TemplateRenderer templateRenderer) {
    this.routers = routers
    this.templateRenderer = templateRenderer
  }

  @Override
  Routing getRouting() {
    this
  }

  void register(String method, String path, @HandlerClosure Closure<?> handler) {
    routers << new PathRouter(path, method, new ClosureBackedResponderFactory(templateRenderer, handler))
  }

  void get(String path, @HandlerClosure Closure<?> handler) {
    register("get", path, handler)
  }

  void post(String path, @HandlerClosure Closure<?> handler) {
    register("post", path, handler)
  }

  void put(String path, @HandlerClosure Closure<?> handler) {
    register("put", path, handler)
  }

  void delete(String path, @HandlerClosure Closure<?> handler) {
    register("delete", path, handler)
  }

  @Override
  Object run() {
    this
  }
}
