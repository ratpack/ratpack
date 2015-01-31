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

package ratpack.groovy.guice;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provider;
import ratpack.func.Action;
import ratpack.guice.BindingsSpec;
import ratpack.guice.ConfigurableModule;

/**
 * Groovy specific extensions to {@link ratpack.guice.BindingsSpec}.
 */
public interface GroovyBindingsSpec extends BindingsSpec {

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyBindingsSpec add(Module module);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyBindingsSpec add(Class<? extends Module> moduleClass);

  /**
   * {@inheritDoc}
   */
  @Override
  <C, T extends ConfigurableModule<C>> GroovyBindingsSpec add(Class<T> moduleClass, Action<? super C> configurer);

  /**
   * {@inheritDoc}
   */
  @Override
  <C> GroovyBindingsSpec add(ConfigurableModule<C> module, Action<? super C> configurer);

  /**
   * {@inheritDoc}
   */
  @Override
  <C, T extends ConfigurableModule<C>> GroovyBindingsSpec addConfig(Class<T> moduleClass, C config, Action<? super C> configurer);

  /**
   * {@inheritDoc}
   */
  @Override
  <C> GroovyBindingsSpec addConfig(ConfigurableModule<C> module, C config, Action<? super C> configurer);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyBindingsSpec binder(Action<? super Binder> action);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyBindingsSpec bind(Class<?> type);

  /**
   * {@inheritDoc}
   */
  @Override
  <T> GroovyBindingsSpec bind(Class<T> publicType, Class<? extends T> implType);

  /**
   * {@inheritDoc}
   */
  @Override
  <T> GroovyBindingsSpec bindInstance(Class<? super T> publicType, T instance);

  /**
   * {@inheritDoc}
   */
  @Override
  <T> GroovyBindingsSpec bindInstance(T instance);

  /**
   * {@inheritDoc}
   */
  @Override
  <T> GroovyBindingsSpec providerType(Class<T> publicType, Class<? extends Provider<? extends T>> providerType);

  /**
   * {@inheritDoc}
   */
  @Override
  <T> GroovyBindingsSpec provider(Class<T> publicType, Provider<? extends T> provider);

}
