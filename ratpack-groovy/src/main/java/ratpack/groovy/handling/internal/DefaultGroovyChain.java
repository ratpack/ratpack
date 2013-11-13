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

package ratpack.groovy.handling.internal;

import com.google.common.collect.ImmutableList;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.api.Nullable;
import ratpack.groovy.handling.GroovyChain;
import ratpack.groovy.internal.Util;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.handling.internal.ChainBuilder;
import ratpack.launch.LaunchConfig;
import ratpack.registry.Registry;
import ratpack.util.Action;

import java.util.List;

import static ratpack.groovy.Groovy.groovyHandler;

public class DefaultGroovyChain extends ratpack.handling.internal.DefaultChain implements GroovyChain {

  public DefaultGroovyChain(List<Handler> handlers, LaunchConfig launchConfig, @Nullable Registry registry) {
    super(handlers, launchConfig, registry);
  }

  @Override
  public GroovyChain handler(Handler handler) {
    return (GroovyChain) super.handler(handler);
  }

  public GroovyChain handler(Closure<?> handler) {
    return handler(groovyHandler(handler));
  }

  public GroovyChain prefix(String prefix, Closure<?> chain) {
    return prefix(prefix, toHandlerList(chain));
  }

  @Override
  public GroovyChain prefix(String prefix, Handler... handlers) {
    return (GroovyChain) super.prefix(prefix, handlers);
  }

  @Override
  public GroovyChain prefix(String prefix, List<Handler> handlers) {
    return (GroovyChain) super.prefix(prefix, handlers);
  }

  @Override
  public GroovyChain prefix(String prefix, Action<? super ratpack.handling.Chain> builder) {
    return (GroovyChain) super.prefix(prefix, builder);
  }

  public GroovyChain handler(String path, Closure<?> handler) {
    return handler(path, groovyHandler(handler));
  }

  @Override
  public GroovyChain handler(String path, Handler handler) {
    return (GroovyChain) super.handler(path, handler);
  }

  public GroovyChain get(String path, Closure<?> handler) {
    return get(path, groovyHandler(handler));
  }

  @Override
  public GroovyChain get(String path, Handler handler) {
    return (GroovyChain) super.get(path, handler);
  }

  @Override
  public GroovyChain get(Handler handler) {
    return (GroovyChain) super.get(handler);
  }

  public GroovyChain get(Closure<?> handler) {
    return get("", handler);
  }

  @Override
  public GroovyChain post(String path, Handler handler) {
    return (GroovyChain) super.post(path, handler);
  }

  public GroovyChain post(String path, Closure<?> handler) {
    return post(path, groovyHandler(handler));
  }

  @Override
  public GroovyChain post(Handler handler) {
    return (GroovyChain) super.post(handler);
  }

  public GroovyChain post(Closure<?> handler) {
    return post("", handler);
  }

  public GroovyChain put(String path, Closure<?> handler) {
    return put(path, groovyHandler(handler));
  }

  @Override
  public GroovyChain put(String path, Handler handler) {
    return (GroovyChain) super.put(path, handler);
  }

  @Override
  public GroovyChain put(Handler handler) {
    return (GroovyChain) super.put(handler);
  }

  public GroovyChain put(Closure<?> handler) {
    return put("", handler);
  }

  public GroovyChain delete(String path, Closure<?> handler) {
    return delete(path, groovyHandler(handler));
  }

  @Override
  public GroovyChain delete(String path, Handler handler) {
    return (GroovyChain) super.delete(path, handler);
  }

  @Override
  public GroovyChain delete(Handler handler) {
    return (GroovyChain) super.delete(handler);
  }

  public GroovyChain delete(Closure<?> handler) {
    return delete("", handler);
  }

  @Override
  public GroovyChain assets(String path, String... indexFiles) {
    return (GroovyChain) super.assets(path, indexFiles);
  }

  public GroovyChain register(Object service, Closure<?> handlers) {
    return register(service, toHandlerList(handlers));
  }

  @Override
  public GroovyChain register(Object service, List<Handler> handlers) {
    return (GroovyChain) super.register(service, handlers);
  }

  public <T> GroovyChain register(Class<? super T> type, T service, Closure<?> handlers) {
    return register(type, service, toHandlerList(handlers));
  }

  @Override
  public <T> GroovyChain register(Class<? super T> type, T service, List<Handler> handlers) {
    return (GroovyChain) super.register(type, service, handlers);
  }

  @Override
  public GroovyChain fileSystem(String path, List<Handler> handlers) {
    return (GroovyChain) super.fileSystem(path, handlers);
  }

  public GroovyChain fileSystem(String path, Closure<?> handlers) {
    return fileSystem(path, toHandlerList(handlers));
  }

  public GroovyChain header(String headerName, String headerValue, Handler handler) {
    return (GroovyChain) super.header(headerName, headerValue, handler);
  }

  public GroovyChain header(String headerName, String headerValue, @DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return header(headerName, headerValue, groovyHandler(handler));
  }

  private ImmutableList<Handler> toHandlerList(Closure<?> handlers) {
    return ChainBuilder.INSTANCE.buildList(new GroovyDslChainActionTransformer(getLaunchConfig(), getRegistry()), Util.delegatingAction(handlers));
  }

}
