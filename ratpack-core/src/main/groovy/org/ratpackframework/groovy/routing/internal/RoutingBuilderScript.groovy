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

package org.ratpackframework.groovy.routing.internal

import org.ratpackframework.Response
import org.ratpackframework.groovy.routing.ClosureRouting
import org.ratpackframework.groovy.routing.HandlerClosure
import org.ratpackframework.Handler
import org.ratpackframework.groovy.routing.internal.ClosureBackedResponseHandler
import org.ratpackframework.routing.internal.RoutingBuilder

public class RoutingBuilderScript extends Script implements ClosureRouting {

  private final RoutingBuilder routingBuilder

  public RoutingBuilderScript(RoutingBuilder routingBuilder) {
    this.routingBuilder = routingBuilder
  }

  @Override
  public ClosureRouting getRouting() {
    return this
  }

  @Override
  public void register(String method, String path, Handler<Response> handler) {
    routingBuilder.register(method, path, handler)
  }

  @Override
  public void all(String path, Handler<Response> handler) {
    routingBuilder.all(path, handler)
  }

  @Override
  public void get(String path, Handler<Response> handler) {
    routingBuilder.get(path, handler)
  }

  @Override
  public void post(String path, Handler<Response> handler) {
    routingBuilder.post(path, handler)
  }

  @Override
  public void put(String path, Handler<Response> handler) {
    routingBuilder.put(path, handler)
  }

  @Override
  public void delete(String path, Handler<Response> handler) {
    routingBuilder.delete(path, handler)
  }

  @Override
  public void delete(String path, @HandlerClosure Closure<?> handler) {
    delete(path, new ClosureBackedResponseHandler(handler))
  }

  @Override
  public void put(String path, @HandlerClosure Closure<?> handler) {
    put(path, new ClosureBackedResponseHandler(handler))
  }

  @Override
  public void post(String path, @HandlerClosure Closure<?> handler) {
    post(path, new ClosureBackedResponseHandler(handler))
  }

  @Override
  public void get(String path, @HandlerClosure Closure<?> handler) {
    get(path, new ClosureBackedResponseHandler(handler))
  }

  @Override
  public void all(String path, @HandlerClosure Closure<?> handler) {
    all(path, new ClosureBackedResponseHandler(handler))
  }

  @Override
  public void register(String method, String path, @HandlerClosure Closure<?> handler) {
    register(method, path, new ClosureBackedResponseHandler(handler))
  }

  @Override
  def <T> T service(Class<T> type) {
    routingBuilder.service(type)
  }

  @Override
  void register(String method, String path, Class<? extends Handler<Response>> handlerType) {
    routingBuilder.register(method, path, handlerType)
  }

  @Override
  void all(String path, Class<? extends Handler<Response>> handlerType) {
    routingBuilder.all(path, handlerType);
  }

  @Override
  void get(String path, Class<? extends Handler<Response>> handlerType) {
    routingBuilder.get(path, handlerType);
  }

  @Override
  void post(String path, Class<? extends Handler<Response>> handlerType) {
    routingBuilder.post(path, handlerType);
  }

  @Override
  void put(String path, Class<? extends Handler<Response>> handlerType) {
    routingBuilder.put(path, handlerType);
  }

  @Override
  void delete(String path, Class<? extends Handler<Response>> handlerType) {
    routingBuilder.delete(path, handlerType);
  }

  @Override
  public Object run() {
    return this
  }

}
