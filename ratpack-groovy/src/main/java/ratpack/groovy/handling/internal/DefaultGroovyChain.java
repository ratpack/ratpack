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
import ratpack.func.Action;
import ratpack.groovy.Groovy;
import ratpack.groovy.handling.GroovyChain;
import ratpack.groovy.internal.ClosureUtil;
import ratpack.handling.Chain;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.launch.LaunchConfig;
import ratpack.registry.Registry;
import ratpack.registry.RegistrySpec;

import static ratpack.groovy.Groovy.groovyHandler;

public class DefaultGroovyChain implements GroovyChain {

  private final Chain delegate;

  public DefaultGroovyChain(Chain delegate) {
    this.delegate = delegate;
  }

  @Override
  public GroovyChain assets(String path, String... indexFiles) {
    delegate.assets(path, indexFiles);
    return this;
  }

  @Override
  public Handler chain(Action<? super Chain> action) throws Exception {
    return delegate.chain(action);
  }

  @Override
  public GroovyChain delete(String path, Closure<?> handler) {
    return delete(path, groovyHandler(handler));
  }

  @Override
  public GroovyChain delete(String path, Handler handler) {
    delegate.delete(path, handler);
    return this;
  }

  @Override
  public GroovyChain delete(Handler handler) {
    delegate.delete(handler);
    return this;
  }

  @Override
  public GroovyChain delete(Closure<?> handler) {
    return delete("", handler);
  }

  @Override
  public GroovyChain fileSystem(String path, Handler handler) {
    delegate.fileSystem(path, handler);
    return this;
  }

  @Override
  public GroovyChain fileSystem(String path, Action<? super Chain> action) throws Exception {
    delegate.fileSystem(path, action);
    return this;
  }

  @Override
  public GroovyChain fileSystem(String path, Closure<?> handlers) throws Exception {
    return fileSystem(path, toHandler(handlers));
  }

  @Override
  public GroovyChain get(String path, Closure<?> handler) {
    return get(path, groovyHandler(handler));
  }

  @Override
  public GroovyChain get(String path, Handler handler) {
    delegate.get(path, handler);
    return this;
  }

  @Override
  public GroovyChain get(Handler handler) {
    delegate.get(handler);
    return this;
  }

  @Override
  public GroovyChain get(Closure<?> handler) {
    return get("", handler);
  }

  @Override
  public LaunchConfig getLaunchConfig() {
    return delegate.getLaunchConfig();
  }

  @Nullable
  @Override
  public Registry getRegistry() {
    return delegate.getRegistry();
  }

  @Override
  public GroovyChain handler(Handler handler) {
    delegate.handler(handler);
    return this;
  }

  @Override
  public GroovyChain handler(Closure<?> handler) {
    return handler(groovyHandler(handler));
  }

  @Override
  public GroovyChain handler(String path, Closure<?> handler) {
    return handler(path, groovyHandler(handler));
  }

  @Override
  public GroovyChain handler(String path, Handler handler) {
    delegate.handler(path, handler);
    return this;
  }

  @Override
  public GroovyChain header(String headerName, String headerValue, Handler handler) {
    delegate.header(headerName, headerValue, handler);
    return this;
  }

  @Override
  public GroovyChain header(String headerName, String headerValue, @DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return header(headerName, headerValue, groovyHandler(handler));
  }

  @Override
  public GroovyChain host(String hostName, Handler handler) {
    delegate.host(hostName, handler);
    return this;
  }

  @Override
  public GroovyChain host(String hostName, @DelegatesTo(value = Context.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) {
    return host(hostName, groovyHandler(handler));
  }

  @Override
  public GroovyChain patch(String path, Closure<?> handler) {
    return patch(path, groovyHandler(handler));
  }

  @Override
  public GroovyChain patch(String path, Handler handler) {
    delegate.patch(path, handler);
    return this;
  }

  @Override
  public GroovyChain patch(Handler handler) {
    delegate.patch(handler);
    return this;
  }

  @Override
  public GroovyChain patch(Closure<?> handler) {
    return patch("", handler);
  }

  @Override
  public GroovyChain post(String path, Handler handler) {
    delegate.post(path, handler);
    return this;
  }

  @Override
  public GroovyChain post(String path, Closure<?> handler) {
    return post(path, groovyHandler(handler));
  }

  @Override
  public GroovyChain post(Handler handler) {
    delegate.post(handler);
    return this;
  }

  @Override
  public GroovyChain post(Closure<?> handler) {
    return post("", handler);
  }

  @Override
  public GroovyChain prefix(String prefix, Handler handler) {
    delegate.prefix(prefix, handler);
    return this;
  }

  @Override
  public GroovyChain prefix(String prefix, Action<? super Chain> action) throws Exception {
    delegate.prefix(prefix, action);
    return this;
  }

  @Override
  public GroovyChain prefix(String prefix, Closure<?> chain) throws Exception {
    return prefix(prefix, toHandler(chain));
  }

  @Override
  public GroovyChain put(String path, Closure<?> handler) {
    return put(path, groovyHandler(handler));
  }

  @Override
  public GroovyChain put(String path, Handler handler) {
    delegate.put(path, handler);
    return this;
  }

  @Override
  public GroovyChain put(Handler handler) {
    delegate.put(handler);
    return this;
  }

  @Override
  public GroovyChain put(Closure<?> handler) {
    return put("", handler);
  }

  @Override
  public GroovyChain register(Registry registry) {
    delegate.register(registry);
    return this;
  }

  @Override
  public GroovyChain register(Action<? super RegistrySpec> action) throws Exception {
    delegate.register(action);
    return this;
  }

  @Override
  public GroovyChain register(Registry registry, Handler handler) {
    delegate.register(registry, handler);
    return this;
  }

  @Override
  public GroovyChain register(Registry registry, Action<? super Chain> action) throws Exception {
    delegate.register(registry, action);
    return this;
  }

  @Override
  public GroovyChain register(Action<? super RegistrySpec> registryAction, Handler handler) throws Exception {
    delegate.register(registryAction, handler);
    return this;
  }

  @Override
  public GroovyChain register(Action<? super RegistrySpec> registryAction, Action<? super Chain> chainAction) throws Exception {
    delegate.register(registryAction, chainAction);
    return this;
  }

  @Override
  public GroovyChain register(Registry registry, @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handlers) throws Exception {
    return register(registry, toHandler(handlers));
  }

  @Override
  public GroovyChain register(@DelegatesTo(value = RegistrySpec.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) throws Exception {
    return register(ClosureUtil.delegatingAction(closure));
  }

  @Override
  public GroovyChain register(Action<? super RegistrySpec> registryAction, @DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> handler) throws Exception {
    return register(registryAction, toHandler(handler));
  }

  private Handler toHandler(Closure<?> handlers) throws Exception {
    return Groovy.chain(getLaunchConfig(), getRegistry(), handlers);
  }

  @Override
  public GroovyChain insert(Action<? super Chain> action) throws Exception {
    delegate.insert(action);
    return this;
  }

  @Override
  public Handler chain(@DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) throws Exception {
    return chain(Groovy.chain(closure));
  }

  @Override
  public GroovyChain insert(@DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) throws Exception {
    insert(Groovy.chain(closure));
    return this;
  }
}
