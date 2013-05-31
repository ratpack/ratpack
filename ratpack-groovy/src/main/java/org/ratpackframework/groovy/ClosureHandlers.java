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

package org.ratpackframework.groovy;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.ratpackframework.handling.Chain;
import org.ratpackframework.handling.Exchange;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.handling.Handlers;
import org.ratpackframework.http.internal.MethodHandler;
import org.ratpackframework.util.Action;

public abstract class ClosureHandlers {

  public static Handler handler(@DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> handler) {
    return new Handler() {
      public void handle(Exchange exchange) {
        Closures.configure(exchange, handler);
      }
    };
  }

  public static Handler context(final Object context, @DelegatesTo(value = Chain.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> handlers) {
    return Handlers.context(context, chain(handlers));
  }

  public static <T> Handler context(Class<? super T> type, T object, @DelegatesTo(value = Chain.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> handlers) {
    return Handlers.context(type, object, chain(handlers));
  }

  private static Action<Chain> chain(@DelegatesTo(value = Chain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers) {
    return Closures.action(Chain.class, handlers);
  }

  public static Handler fileSystem(String path, @DelegatesTo(value = Chain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers) {
    return Handlers.fileSystem(path, chain(handlers));
  }

  public static Handler path(String path, @DelegatesTo(value = Chain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers) {
    return Handlers.prefix(path, chain(handlers));
  }

  public static Handler handler(String path, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    return Handlers.path(path, handler(closure));
  }

  public static Handler get(String path, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    return Handlers.path(path, MethodHandler.GET, handler(closure));
  }

  public static Handler get(@DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    return Handlers.path("", MethodHandler.GET, handler(closure));
  }

  public static Handler post(String path, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    return Handlers.path(path, MethodHandler.POST, handler(closure));
  }

  public static Handler put(String path, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    return Handlers.path(path, MethodHandler.PUT, handler(closure));
  }

  public static Handler delete(String path, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    return Handlers.path(path, MethodHandler.DELETE, handler(closure));
  }

}
