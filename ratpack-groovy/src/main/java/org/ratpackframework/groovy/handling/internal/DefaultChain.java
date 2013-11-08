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
import groovy.lang.DelegatesTo;
import org.ratpackframework.api.Nullable;
import org.ratpackframework.groovy.Util;
import org.ratpackframework.groovy.handling.Chain;
import org.ratpackframework.handling.Context;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.handling.internal.ChainBuilder;
import org.ratpackframework.launch.LaunchConfig;
import org.ratpackframework.registry.Registry;
import org.ratpackframework.util.Action;

import java.util.List;

import static org.ratpackframework.groovy.Util.asHandler;

public class DefaultChain extends org.ratpackframework.handling.internal.DefaultChain implements Chain {

  public DefaultChain(List<Handler> handlers, LaunchConfig launchConfig, @Nullable Registry registry) {
    super(handlers, launchConfig, registry);
  }

  @Override
  public Chain handler(Handler handler) {
    return (Chain) super.handler(handler);
  }

  public Chain handler(Closure<?> handler) {
    return handler(asHandler(handler));
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

  public Chain handler(String path, Closure<?> handler) {
    return handler(path, asHandler(handler));
  }

  @Override
  public Chain handler(String path, Handler handler) {
    return (Chain) super.handler(path, handler);
  }

  public Chain get(String path, Closure<?> handler) {
    return get(path, asHandler(handler));
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
    return post(path, asHandler(handler));
  }

  @Override
  public Chain post(Handler handler) {
    return (Chain) super.post(handler);
  }

  public Chain post(Closure<?> handler) {
    return post("", handler);
  }

  public Chain put(String path, Closure<?> handler) {
    return put(path, asHandler(handler));
  }

  @Override
  public Chain put(String path, Handler handler) {
    return (Chain) super.put(path, handler);
  }

  @Override
  public Chain put(Handler handler) {
    return (Chain) super.put(handler);
  }

  public Chain put(Closure<?> handler) {
    return put("", handler);
  }

  public Chain delete(String path, Closure<?> handler) {
    return delete(path, asHandler(handler));
  }

  @Override
  public Chain delete(String path, Handler handler) {
    return (Chain) super.delete(path, handler);
  }

  @Override
  public Chain delete(Handler handler) {
    return (Chain) super.delete(handler);
  }

  public Chain delete(Closure<?> handler) {
    return delete("", handler);
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

  @Override
  public Chain header(String headerName, String headerValue, Handler handler) {
    return (Chain) super.header(headerName, headerValue, handler);
  }

  public Chain header(String headerName, String headerValue, @DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return header(headerName, headerValue, asHandler(handler));
  }

  @Override
  public Chain soapAction(String value, Handler handler) {
    return (Chain) super.soapAction(value, handler);
  }

  public Chain soapAction(String value, @DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return soapAction(value, asHandler(handler));
  }

  private ImmutableList<Handler> toHandlerList(Closure<?> handlers) {
    return ChainBuilder.INSTANCE.buildList(new GroovyDslChainActionTransformer(getLaunchConfig(), getRegistry()), Util.delegatingAction(handlers));
  }

}
