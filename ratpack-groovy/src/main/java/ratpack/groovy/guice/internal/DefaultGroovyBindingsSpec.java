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

package ratpack.groovy.guice.internal;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provider;
import ratpack.func.Action;
import ratpack.groovy.guice.GroovyBindingsSpec;
import ratpack.guice.BindingsSpec;
import ratpack.guice.ConfigurableModule;
import ratpack.guice.NoSuchModuleException;
import ratpack.server.ServerConfig;

import java.util.function.Consumer;

public class DefaultGroovyBindingsSpec implements GroovyBindingsSpec {

  private final BindingsSpec delegate;

  public DefaultGroovyBindingsSpec(BindingsSpec delegate) {
    this.delegate = delegate;
  }

  @Override
  public ServerConfig getServerConfig() {
    return delegate.getServerConfig();
  }

  @Override
  public GroovyBindingsSpec bind(Class<?> type) {
    delegate.bind(type);
    return this;
  }

  @Override
  public <T> GroovyBindingsSpec bind(Class<T> publicType, Class<? extends T> implType) {
    delegate.bind(publicType, implType);
    return this;
  }

  @Override
  public <T> GroovyBindingsSpec bindInstance(Class<? super T> publicType, T instance) {
    delegate.bindInstance(publicType, instance);
    return this;
  }

  @Override
  public <T> GroovyBindingsSpec bindInstance(T instance) {
    delegate.bindInstance(instance);
    return this;
  }

  @Override
  public <T> GroovyBindingsSpec providerType(Class<T> publicType, Class<? extends Provider<? extends T>> providerType) {
    delegate.providerType(publicType, providerType);
    return this;
  }

  @Override
  public <T> GroovyBindingsSpec provider(Class<T> publicType, Provider<? extends T> provider) {
    delegate.provider(publicType, provider);
    return this;
  }

  @Override
  public <T extends Module> GroovyBindingsSpec config(Class<T> moduleClass, Consumer<? super T> configurer) throws NoSuchModuleException {
    delegate.config(moduleClass, configurer);
    return this;
  }

  @Override
  public GroovyBindingsSpec binder(Action<? super Binder> action) {
    delegate.binder(action);
    return this;
  }

  @Override
  public GroovyBindingsSpec add(Module module) {
    delegate.add(module);
    return this;
  }

  @SuppressWarnings("unchecked")
  @Override
  public GroovyBindingsSpec add(Class<? extends Module> module) {
    delegate.add(module);
    return this;
  }

  @Override
  public <C> BindingsSpec add(ConfigurableModule<C> module, Action<? super C> configuration) {
    delegate.add(module, configuration);
    return this;
  }

  @Override
  public <C, T extends ConfigurableModule<C>> GroovyBindingsSpec add(Class<T> moduleClass, Action<? super C> configuration) {
    delegate.add(moduleClass, configuration);
    return this;
  }
}
