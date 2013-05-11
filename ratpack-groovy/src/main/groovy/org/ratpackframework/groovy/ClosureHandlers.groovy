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

package org.ratpackframework.groovy

import org.ratpackframework.Action
import org.ratpackframework.routing.Exchange
import org.ratpackframework.routing.Handler
import org.ratpackframework.routing.Handlers
import org.ratpackframework.routing.Routing

public abstract class ClosureHandlers {

  public static Handler handler(@DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> handler) {
    new Handler() {
      @Override
      public void handle(Exchange exchange) {
        Closures.configure(exchange, handler);
      }
    }
  }

  public static Handler context(final Object context, @DelegatesTo(value = Routing.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> routes) {
    Handlers.context(context, routingAction(routes));
  }

  private static Action<Routing> routingAction(@DelegatesTo(value = Routing.class, strategy = Closure.DELEGATE_FIRST) Closure<?> routes) {
    Closures.action(Routing.class, routes)
  }

  public static Handler fsContext(String path, @DelegatesTo(value = Routing.class, strategy = Closure.DELEGATE_FIRST) Closure routing) {
    Handlers.fsContext(path, routingAction(routing))
  }

  public static Handler path(String path, @DelegatesTo(value = Routing.class, strategy = Closure.DELEGATE_FIRST) Closure routing) {
    Handlers.path(path, routingAction(routing))
  }

  public static Handler exactPath(String path, @DelegatesTo(value = Routing.class, strategy = Closure.DELEGATE_FIRST) Closure routing) {
    Handlers.exactPath(path, routingAction(routing))
  }

  public static Handler method(Collection<String> methods, @DelegatesTo(value = Routing.class, strategy = Closure.DELEGATE_FIRST) Closure routing) {
    Handlers.method(methods, routingAction(routing))
  }

  public static Handler handler(String path, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    Handlers.path(path, handler(closure))
  }

  public static Handler handler(String path, String method, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    handler(path, Arrays.asList(method), closure)
  }

  public static Handler handler(String path, Collection<String> methods, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    Handlers.exactPath(path, Handlers.method(methods, handler(closure)))
  }

  public static Handler get(String path, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    handler(path, "get", closure)
  }

  public static Handler post(String path, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    handler(path, "post", closure)
  }

  public static Handler put(String path, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    handler(path, "put", closure)
  }

  public static Handler delete(String path, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    handler(path, "delete", closure)
  }

}
