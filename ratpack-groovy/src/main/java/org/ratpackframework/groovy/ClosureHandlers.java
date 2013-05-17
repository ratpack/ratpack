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
import org.ratpackframework.handling.Exchange;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.handling.Handlers;
import org.ratpackframework.handling.ChainBuilder;
import org.ratpackframework.util.Action;

import java.util.Arrays;
import java.util.Collection;

public abstract class ClosureHandlers {

  public static Handler handler(@DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> handler) {
    return new Handler() {
      public void handle(Exchange exchange) {
        Closures.configure(exchange, handler);
      }
    };
  }

  public static Handler context(final Object context, @DelegatesTo(value = ChainBuilder.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> handlers) {
    return Handlers.context(context, chain(handlers));
  }

  private static Action<ChainBuilder> chain(@DelegatesTo(value = ChainBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers) {
    return Closures.action(ChainBuilder.class, handlers);
  }

  public static Handler fsContext(String path, @DelegatesTo(value = ChainBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers) {
    return Handlers.fsContext(path, chain(handlers));
  }

  public static Handler path(String path, @DelegatesTo(value = ChainBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers) {
    return Handlers.path(path, chain(handlers));
  }

  public static Handler exactPath(String path, @DelegatesTo(value = ChainBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers) {
    return Handlers.exactPath(path, chain(handlers));
  }

  public static Handler method(Collection<String> methods, @DelegatesTo(value = ChainBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers) {
    return Handlers.method(methods, chain(handlers));
  }

  public static Handler handler(String path, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    return Handlers.path(path, handler(closure));
  }

  public static Handler handler(String path, String method, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    return handler(path, Arrays.asList(method), closure);
  }

  public static Handler handler(String path, Collection<String> methods, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    return Handlers.exactPath(path, Handlers.method(methods, handler(closure)));
  }

  public static Handler get(String path, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    return handler(path, "get", closure);
  }

  public static Handler post(String path, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    return handler(path, "post", closure);
  }

  public static Handler put(String path, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    return handler(path, "put", closure);
  }

  public static Handler delete(String path, @DelegatesTo(value = Exchange.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> closure) {
    return handler(path, "delete", closure);
  }

}
