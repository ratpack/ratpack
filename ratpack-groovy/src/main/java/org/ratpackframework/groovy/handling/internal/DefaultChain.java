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
import org.ratpackframework.util.Action;

import java.util.List;

public class DefaultChain extends org.ratpackframework.handling.internal.DefaultChain implements Chain {

  public DefaultChain(List<Handler> handlers, @Nullable Registry<Object> registry) {
    super(handlers, registry);
  }

  public Chain handler(Handler handler) {
    return (Chain) super.handler(handler);
  }

  public Chain handler(Closure<?> handler) {
    return handler(new ClosureBackedHandler(handler));
  }

  public Chain prefix(String prefix, Closure<?> chain) {
    return prefix(prefix, toHandlerList(chain));
  }

  public Chain prefix(String prefix, Handler... handlers) {
    return (Chain) super.prefix(prefix, handlers);
  }

  public Chain prefix(String prefix, List<Handler> handlers) {
    return (Chain) super.prefix(prefix, handlers);
  }

  public Chain prefix(String prefix, Handler handler) {
    return (Chain) super.prefix(prefix, handler);
  }

  public Chain prefix(String prefix, Action<? super org.ratpackframework.handling.Chain> chainAction) {
    return (Chain) super.prefix(prefix, chainAction);
  }

  public Chain path(String path, Closure<?> handler) {
    return path(path, new ClosureBackedHandler(handler));
  }

  public Chain path(String path, Handler handler) {
    return (Chain) super.path(path, handler);
  }

  public Chain get(String path, Closure<?> handler) {
    return get(path, new ClosureBackedHandler(handler));
  }

  public Chain get(String path, Handler handler) {
    return (Chain) super.get(path, handler);
  }

  public Chain get(Handler handler) {
    return (Chain) super.get(handler);
  }

  public Chain get(Closure<?> handler) {
    return get("", handler);
  }

  public Chain post(String path, Handler handler) {
    return (Chain) super.post(path, handler);
  }

  public Chain post(String path, Closure<?> handler) {
    return post(path, new ClosureBackedHandler(handler));
  }

  public Chain post(Handler handler) {
    return (Chain) super.post(handler);
  }

  public Chain post(Closure<?> handler) {
    return post("", handler);
  }

  @Override
  public Chain assets(String path, String... indexFiles) {
    return (Chain) super.assets(path, indexFiles);
  }

  public Chain assets(String path, Handler handler) {
    return (Chain) super.assets(path, handler);
  }

  public Chain assets(String path, Closure<?> handler) {
    return assets(path, new ClosureBackedHandler(handler));
  }

  public Chain register(Object object, Closure<?> handlers) {
    return register(object, toHandlerList(handlers));
  }

  public Chain register(Object object, List<Handler> handlers) {
    return (Chain) super.register(object, handlers);
  }

  public <T> Chain register(Class<? super T> type, T object, Closure<?> handlers) {
    return register(type, object, toHandlerList(handlers));
  }

  public <T> Chain register(Class<? super T> type, T object, List<Handler> handlers) {
    return (Chain) super.register(type, object, handlers);
  }

  @Override
  public Chain fileSystem(String path, List<Handler> handlers) {
    return (Chain) super.fileSystem(path, handlers);
  }

  public Chain fileSystem(String path, Closure<?> handlers) {
    return fileSystem(path, toHandlerList(handlers));
  }

  private ImmutableList<Handler> toHandlerList(Closure<?> handlers) {
    return ChainBuilder.INSTANCE.buildList(new GroovyDslChainActionTransformer(getRegistry()), Util.delegatingAction(handlers));
  }

}
