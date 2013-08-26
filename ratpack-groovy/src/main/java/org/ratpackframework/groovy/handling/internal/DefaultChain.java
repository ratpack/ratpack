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

  @Override
  public Chain handler(Handler handler) {
    return (Chain) super.handler(handler);
  }

  public Chain handler(Closure<?> handler) {
    return handler(new ClosureBackedHandler(handler));
  }

  public Chain prefix(String prefix, Closure<?> chain) {
    return prefix(prefix, toHandlerList(chain));
  }

  @Override
  public Chain prefix(String prefix, Handler... handlers) {
    return (Chain) super.prefix(prefix, handlers);
  }

  @Override
  public Chain prefix(String prefix, List<Handler> handlers) {
    return (Chain) super.prefix(prefix, handlers);
  }

  @Override
  public Chain prefix(String prefix, Action<? super org.ratpackframework.handling.Chain> builder) {
    return (Chain) super.prefix(prefix, builder);
  }

  public Chain path(String path, Closure<?> handler) {
    return handler(path, new ClosureBackedHandler(handler));
  }

  @Override
  public Chain handler(String path, Handler handler) {
    return (Chain) super.handler(path, handler);
  }

  public Chain get(String path, Closure<?> handler) {
    return get(path, new ClosureBackedHandler(handler));
  }

  @Override
  public Chain get(String path, Handler handler) {
    return (Chain) super.get(path, handler);
  }

  @Override
  public Chain get(Handler handler) {
    return (Chain) super.get(handler);
  }

  public Chain get(Closure<?> handler) {
    return get("", handler);
  }

  @Override
  public Chain post(String path, Handler handler) {
    return (Chain) super.post(path, handler);
  }

  public Chain post(String path, Closure<?> handler) {
    return post(path, new ClosureBackedHandler(handler));
  }

  @Override
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

  public Chain register(Object service, Closure<?> handlers) {
    return register(service, toHandlerList(handlers));
  }

  @Override
  public Chain register(Object service, List<Handler> handlers) {
    return (Chain) super.register(service, handlers);
  }

  public <T> Chain register(Class<? super T> type, T service, Closure<?> handlers) {
    return register(type, service, toHandlerList(handlers));
  }

  @Override
  public <T> Chain register(Class<? super T> type, T service, List<Handler> handlers) {
    return (Chain) super.register(type, service, handlers);
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
