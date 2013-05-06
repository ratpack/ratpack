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

package org.ratpackframework.groovy.routing

import org.ratpackframework.http.Exchange
import org.ratpackframework.http.Response


public interface Routing extends org.ratpackframework.routing.Routing {

  Routing getRouting()

  void route(@DelegatesTo(value = Exchange, strategy = Closure.DELEGATE_FIRST) Closure<?> handler)

  void path(String path, @DelegatesTo(value = Exchange, strategy = Closure.DELEGATE_FIRST) Closure<?> handler)

  /**
   * Delegates {@link #route(java.lang.String, java.lang.String, groovy.lang.Closure)} with a method of "get"
   */
  void get(String path, @DelegatesTo(value = Response, strategy = Closure.DELEGATE_FIRST) Closure<?> handler)

  /**
   * Delegates {@link #route(java.lang.String, java.lang.String, groovy.lang.Closure)} with a method of "post"
   */
  void post(String path, @DelegatesTo(value = Response, strategy = Closure.DELEGATE_FIRST) Closure<?> handler)

  /**
   * Delegates {@link #route(java.lang.String, java.lang.String, groovy.lang.Closure)} with a method of "put"
   */
  void put(String path, @DelegatesTo(value = Response, strategy = Closure.DELEGATE_FIRST) Closure<?> handler)

  /**
   * Delegates {@link #route(java.lang.String, java.lang.String, groovy.lang.Closure)} with a method of "delete"
   */
  void delete(String path, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler)

}
