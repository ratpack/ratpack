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

import com.google.common.collect.ImmutableList;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.ratpackframework.groovy.Util;
import org.ratpackframework.groovy.handling.internal.ClosureBackedHandler;
import org.ratpackframework.handling.Chain;
import org.ratpackframework.handling.Context;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.handling.Handlers;
import org.ratpackframework.util.Action;

public abstract class ClosureHandlers {

  public static Handler handler(@DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_ONLY) final Closure<?> handler) {
    return new ClosureBackedHandler(handler);
  }

  public static Handler register(final Object service, @DelegatesTo(value = Chain.class, strategy = Closure.DELEGATE_ONLY) final Closure<?> handlers) {
    return Handlers.service(service, chain(handlers));
  }

  public static <T> Handler register(Class<? super T> type, T object, @DelegatesTo(value = Chain.class, strategy = Closure.DELEGATE_ONLY) final Closure<?> handlers) {
    return Handlers.service(type, object, chain(handlers));
  }

  private static Action<Chain> chain(@DelegatesTo(value = Chain.class, strategy = Closure.DELEGATE_ONLY) Closure<?> handlers) {
    return Util.action(Chain.class, handlers);
  }

  public static Handler fileSystem(String path, @DelegatesTo(value = Chain.class, strategy = Closure.DELEGATE_ONLY) Closure<?> handlers) {
    return Handlers.fileSystem(path, chain(handlers));
  }

  public static Handler path(String path, @DelegatesTo(value = Chain.class, strategy = Closure.DELEGATE_ONLY) Closure<?> handlers) {
    return Handlers.prefix(path, chain(handlers));
  }

  public static Handler handler(String path, @DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_ONLY) final Closure<?> closure) {
    return Handlers.path(path, handler(closure));
  }

  public static Handler get(String path, @DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_ONLY) final Closure<?> closure) {
    return Handlers.path(path, ImmutableList.<Handler>builder().add(Handlers.get(), handler(closure)).build());
  }

  public static Handler get(@DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_ONLY) final Closure<?> closure) {
    return Handlers.path("", ImmutableList.<Handler>builder().add(Handlers.get(), handler(closure)).build());
  }

  public static Handler post(String path, @DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_ONLY) final Closure<?> closure) {
    return Handlers.path(path, ImmutableList.<Handler>builder().add(Handlers.post(), handler(closure)).build());
  }

  public static Handler put(String path, @DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_ONLY) final Closure<?> closure) {
    return Handlers.path(path, ImmutableList.<Handler>builder().add(Handlers.put(), handler(closure)).build());
  }

  public static Handler delete(String path, @DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_ONLY) final Closure<?> closure) {
    return Handlers.path(path, ImmutableList.<Handler>builder().add(Handlers.delete(), handler(closure)).build());
  }

}
