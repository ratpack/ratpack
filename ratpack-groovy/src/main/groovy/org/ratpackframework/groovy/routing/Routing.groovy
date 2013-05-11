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

package org.ratpackframework.groovy.routing

import org.ratpackframework.routing.Exchange

public interface Routing extends org.ratpackframework.routing.Routing {

  void route(@DelegatesTo(value = Exchange, strategy = Closure.DELEGATE_FIRST) Closure<?> handler)

  void routes(@DelegatesTo(value = Exchange, strategy = Closure.DELEGATE_FIRST) Closure<?> handler)

  void all(String path, @DelegatesTo(value = Exchange, strategy = Closure.DELEGATE_FIRST) Closure<?> handler)

  void handler(String path, List<String> methods, @DelegatesTo(value = Exchange, strategy = Closure.DELEGATE_FIRST) Closure<?> handler)

  void path(String path, @DelegatesTo(value = Routing, strategy = Closure.DELEGATE_FIRST) Closure<?> handler)

  void get(String path, @DelegatesTo(value = Exchange, strategy = Closure.DELEGATE_FIRST) Closure<?> handler)

  void get(@DelegatesTo(value = Exchange, strategy = Closure.DELEGATE_FIRST) Closure<?> handler)

  void post(String path, @DelegatesTo(value = Exchange, strategy = Closure.DELEGATE_FIRST) Closure<?> handler)

  void post(@DelegatesTo(value = Exchange, strategy = Closure.DELEGATE_FIRST) Closure<?> handler)

  void assets(String path, String... indexFiles)

  void fsContext(String path, @DelegatesTo(value = Routing, strategy = Closure.DELEGATE_FIRST) Closure<?> routing)

}
