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
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.func.Action;
import ratpack.guice.BindingsSpec;
import ratpack.guice.NoSuchModuleException;

import java.util.function.Consumer;

/**
 * Groovy specific extensions to {@link ratpack.guice.BindingsSpec}.
 */
public interface GroovyBindingsSpec extends BindingsSpec {

  /**
   * Adds a closure based application initializer.
   * <p>
   * The closure can declare parameters, that will be injected.
   * That is, parameters must be typed and implementations of such types must be provided by the modules.
   *
   * @param closure The initializer
   */
  GroovyBindingsSpec init(@DelegatesTo(value = Void.class, strategy = Closure.OWNER_ONLY) Closure<?> closure);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyBindingsSpec add(Module... modules);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyBindingsSpec add(Iterable<? extends Module> modules);

  /**
   * {@inheritDoc}
   */
  @Override
  <T extends Module> GroovyBindingsSpec config(Class<T> moduleClass, Consumer<? super T> configurer) throws NoSuchModuleException;

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

  /**
   * {@inheritDoc}
   * @param action
   */
  @Override
  GroovyBindingsSpec init(Action<? super Injector> action);

  /**
   * {@inheritDoc}
   */
  @Override
  GroovyBindingsSpec init(Class<? extends Runnable> clazz);

}
