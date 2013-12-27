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

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.api.Nullable;
import ratpack.groovy.Groovy;
import ratpack.groovy.handling.GroovyChain;
import ratpack.handling.Chain;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.handling.internal.DefaultChain;
import ratpack.launch.LaunchConfig;
import ratpack.registry.Registry;
import ratpack.util.Action;

import java.util.List;

import static ratpack.groovy.Groovy.groovyHandler;

public class DefaultGroovyChain extends DefaultChain implements GroovyChain {

  public DefaultGroovyChain(List<Handler> handlers, LaunchConfig launchConfig, @Nullable Registry registry) {
    super(handlers, launchConfig, registry);
  }

  @Override
  public GroovyChain handler(Handler handler) {
    return (GroovyChain) super.handler(handler);
  }

  @Override
  public GroovyChain handler(Closure<?> handler) {
    return handler(groovyHandler(handler));
  }

  @Override
  public GroovyChain prefix(String prefix, Handler handler) {
    return (GroovyChain) super.prefix(prefix, handler);
  }

  @Override
  public GroovyChain prefix(String prefix, Action<? super Chain> action) throws Exception {
    return (GroovyChain) super.prefix(prefix, action);
  }

  @Override
  public GroovyChain prefix(String prefix, Closure<?> chain) throws Exception {
    return prefix(prefix, toHandler(chain));
  }

  @Override
  public GroovyChain handler(String path, Closure<?> handler) {
    return handler(path, groovyHandler(handler));
  }

  @Override
  public GroovyChain handler(String path, Handler handler) {
    return (GroovyChain) super.handler(path, handler);
  }

  @Override
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

  @Override
  public GroovyChain get(Closure<?> handler) {
    return get("", handler);
  }

  @Override
  public GroovyChain post(String path, Handler handler) {
    return (GroovyChain) super.post(path, handler);
  }

  @Override
  public GroovyChain post(String path, Closure<?> handler) {
    return post(path, groovyHandler(handler));
  }

  @Override
  public GroovyChain post(Handler handler) {
    return (GroovyChain) super.post(handler);
  }

  @Override
  public GroovyChain post(Closure<?> handler) {
    return post("", handler);
  }

  @Override
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

  @Override
  public GroovyChain put(Closure<?> handler) {
    return put("", handler);
  }

  @Override
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

  @Override
  public GroovyChain delete(Closure<?> handler) {
    return delete("", handler);
  }

  @Override
  public GroovyChain assets(String path, String... indexFiles) {
    return (GroovyChain) super.assets(path, indexFiles);
  }

  @Override
  public GroovyChain register(Object service, Handler handler) {
    return (GroovyChain) super.register(service, handler);
  }

  @Override
  public GroovyChain register(Object service, Action<? super Chain> action) throws Exception {
    return (GroovyChain) super.register(service, action);
  }

  @Override
  public GroovyChain register(Object service, Closure<?> handlers) throws Exception {
    return register(service, toHandler(handlers));
  }

  @Override
  public <T> GroovyChain register(Class<? super T> type, T service, Handler handler) {
    return (GroovyChain) super.register(type, service, handler);
  }

  @Override
  public <T> GroovyChain register(Class<? super T> type, T service, Action<? super Chain> action) throws Exception {
    return (GroovyChain) super.register(type, service, action);
  }

  @Override
  public <T> GroovyChain register(Class<? super T> type, T service, Closure<?> handlers) throws Exception {
    return register(type, service, toHandler(handlers));
  }

  @Override
  public GroovyChain fileSystem(String path, Handler handler) {
    return (GroovyChain) super.fileSystem(path, handler);
  }

  @Override
  public GroovyChain fileSystem(String path, Action<? super Chain> action) throws Exception {
    return (GroovyChain) super.fileSystem(path, action);
  }

  @Override
  public GroovyChain fileSystem(String path, Closure<?> handlers) throws Exception {
    return fileSystem(path, toHandler(handlers));
  }

  @Override
  public GroovyChain header(String headerName, String headerValue, Handler handler) {
    return (GroovyChain) super.header(headerName, headerValue, handler);
  }

  @Override
  public GroovyChain header(String headerName, String headerValue, @DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return header(headerName, headerValue, groovyHandler(handler));
  }

  private Handler toHandler(Closure<?> handlers) throws Exception {
    return Groovy.chain(getLaunchConfig(), getRegistry(), handlers);
  }

}
