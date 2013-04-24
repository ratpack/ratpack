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

package org.ratpackframework.groovy.app.internal

import org.ratpackframework.app.Endpoint
import org.ratpackframework.groovy.app.Routing

// TODO: rewrite as Java
public class RoutingScript extends Script implements Routing {

  private final org.ratpackframework.app.Routing delegate

  public RoutingScript(org.ratpackframework.app.Routing delegate) {
    this.delegate = delegate
  }

  @Override
  public Routing getRouting() {
    return this
  }

  @Override
  org.ratpackframework.Objects getObjects() {
    delegate.getObjects()
  }

  @Override
  Endpoint inject(Class<? extends Endpoint> endpointType) {
    delegate.inject(endpointType)
  }

  @Override
  void route(String method, String path, Closure<?> endpoint) {
    route(method, path, new ClosureEndpoint(endpoint))
  }

  @Override
  void all(String path, Closure<?> endpoint) {
    all(path, new ClosureEndpoint(endpoint))
  }

  @Override
  void get(String path, Closure<?> endpoint) {
    get(path, new ClosureEndpoint(endpoint))
  }

  @Override
  void post(String path, Closure<?> endpoint) {
    post(path, new ClosureEndpoint(endpoint))
  }

  @Override
  void put(String path, Closure<?> endpoint) {
    put(path, new ClosureEndpoint(endpoint))
  }

  @Override
  void delete(String path, Closure<?> endpoint) {
    delete(path, new ClosureEndpoint(endpoint))
  }

  @Override
  void routeRe(String method, String pattern, Closure<?> endpoint) {
    routeRe(method, pattern, new ClosureEndpoint(endpoint))
  }

  @Override
  void allRe(String path, Closure<?> endpoint) {
    allRe(path, new ClosureEndpoint(endpoint))
  }

  @Override
  void getRe(String path, Closure<?> endpoint) {
    getRe(path, new ClosureEndpoint(endpoint))
  }

  @Override
  void postRe(String path, Closure<?> endpoint) {
    postRe(path, new ClosureEndpoint(endpoint))
  }

  @Override
  void putRe(String path, Closure<?> endpoint) {
    putRe(path, new ClosureEndpoint(endpoint))
  }

  @Override
  void deleteRe(String path, Closure<?> endpoint) {
    deleteRe(path, new ClosureEndpoint(endpoint))
  }

  @Override
  void route(String method, String path, Endpoint endpoint) {
    delegate.route(method, path, endpoint)
  }

  @Override
  void routeRe(String method, String pattern, Endpoint endpoint) {
    delegate.routeRe(method, path, endpoint)
  }

  @Override
  void all(String path, Endpoint endpoint) {
    delegate.all(path, endpoint)
  }

  @Override
  void allRe(String path, Endpoint endpoint) {
    delegate.allRe(path, endpoint)
  }

  @Override
  void get(String path, Endpoint endpoint) {
    delegate.get(path, endpoint)
  }

  @Override
  void getRe(String pattern, Endpoint endpoint) {
    delegate.getRe(pattern, endpoint)
  }

  @Override
  void post(String path, Endpoint endpoint) {
    delegate.post(path, endpoint)
  }

  @Override
  void postRe(String pattern, Endpoint endpoint) {
    delegate.postRe(pattern, endpoint)
  }

  @Override
  void put(String path, Endpoint endpoint) {
    delegate.put(path, endpoint)
  }

  @Override
  void putRe(String pattern, Endpoint endpoint) {
    delegate.putRe(pattern, endpoint)
  }

  @Override
  void delete(String path, Endpoint endpoint) {
    delegate.delete(path, endpoint)
  }

  @Override
  void deleteRe(String pattern, Endpoint endpoint) {
    delegate.deleteRe(pattern, endpoint)
  }

  @Override
  public Object run() {
    return this
  }

}
