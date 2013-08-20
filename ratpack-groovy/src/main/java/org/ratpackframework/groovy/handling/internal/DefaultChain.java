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

package org.ratpackframework.groovy.handling.internal;

import com.google.common.collect.ImmutableList;
import groovy.lang.Closure;
import org.ratpackframework.api.Nullable;
import org.ratpackframework.groovy.Util;
import org.ratpackframework.groovy.handling.Chain;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.handling.internal.ChainBuilder;
import org.ratpackframework.registry.Registry;

import java.util.List;

public class DefaultChain extends org.ratpackframework.handling.internal.DefaultChain implements Chain {

  public DefaultChain(List<Handler> handlers, @Nullable Registry<Object> registry) {
    super(handlers, registry);
  }

  public void handler(Closure<?> handler) {
    handler(new ClosureBackedHandler(handler));
  }

  public void prefix(String prefix, Closure<?> chain) {
    prefix(prefix, toHandlerList(chain));
  }

  public void path(String path, Closure<?> handler) {
    path(path, new ClosureBackedHandler(handler));
  }

  public Handler get(String path, Closure<?> handler) {
    return get(path, new ClosureBackedHandler(handler));
  }

  public Handler get(Closure<?> handler) {
    return get("", handler);
  }

  public void post(String path, Closure<?> handler) {
    post(path, new ClosureBackedHandler(handler));
  }

  public void post(Closure<?> handler) {
    post("", handler);
  }

  public void assets(String path, Closure<?> handler) {
    assets(path, new ClosureBackedHandler(handler));
  }

  public Handler register(Object object, Closure<?> handlers) {
    return register(object, toHandlerList(handlers));
  }

  public <T> Handler register(Class<? super T> type, T object, Closure<?> handlers) {
    return register(type, object, toHandlerList(handlers));
  }

  public void fileSystem(String path, Closure<?> handlers) {
    fileSystem(path, toHandlerList(handlers));
  }

  private ImmutableList<Handler> toHandlerList(Closure<?> handlers) {
    return ChainBuilder.INSTANCE.buildList(new GroovyDslChainActionTransformer(getRegistry()), Util.delegatingAction(handlers));
  }

}
