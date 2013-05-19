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

package org.ratpackframework.groovy.handling;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.ratpackframework.handling.Exchange;

import java.util.List;

public interface ChainBuilder extends org.ratpackframework.handling.ChainBuilder {

  void handler(@DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);

  void chain(@DelegatesTo(value = ChainBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);

  void all(String path, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);

  void handler(String path, List<String> methods, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);

  void handler(String path, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);

  void path(String path, @DelegatesTo(value = org.ratpackframework.groovy.handling.ChainBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);

  void get(String path, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);

  void get(@DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);

  void post(String path, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);

  void post(@DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler);

  void assets(String path, String... indexFiles);

  void context(Object object, @DelegatesTo(value = org.ratpackframework.groovy.handling.ChainBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers);

  void fsContext(String path, @DelegatesTo(value = org.ratpackframework.groovy.handling.ChainBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers);

}
