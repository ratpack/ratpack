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

package ratpack.guice;

import com.google.common.reflect.TypeToken;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import ratpack.func.Action;
import ratpack.guice.internal.GuiceUtil;
import ratpack.registry.RegistrySpec;
import ratpack.registry.internal.TypeCaching;
import ratpack.server.ServerConfig;
import ratpack.util.Types;

import java.util.function.Supplier;

import static ratpack.util.Exceptions.uncheck;

/**
 * A buildable specification of Guice bindings.
 * <p>
 * This type is used when bootstrapping a Guice based application to add {@link Module modules} and bindings.
 * <p>
 * It is recommended to become familiar with Guice bindings, scopes and other concerns before using Guice with Ratpack.
 * <h3>Module order and overrides</h3>
 * <p>
 * The order in which modules are added is significant.
 * Subsequent modules can <b>override</b> the bindings of previous modules.
 * This is a very useful technique for augmenting/customising the functionality provided by modules.
 * Many modules provide extensive bindings to facilitate such overriding.
 * <p>
 * Bindings added via the {@code bind()} and {@code provider()} methods always have the highest precedence, regardless of order.
 * That is, non module bindings can always override module bindings regardless of whether the module is added before or after the non module binding.
 */
public interface BindingsSpec extends RegistrySpec {

  /**
   * The launch config for the application.
   *
   * @return the launch config for the application
   */
  ServerConfig getServerConfig();

  /**
   * Adds the bindings from the given module.
   *
   * @param module module whose bindings should be added
   * @return this
   */
  BindingsSpec module(Module module);

  /**
   * Adds the bindings from the given module.
   *
   * @param moduleClass type of the module whose bindings should be added
   * @return this
   */
  @SuppressWarnings("unchecked")
  BindingsSpec module(Class<? extends Module> moduleClass);

  /**
   * Adds the bindings from the given configurable module.
   *
   * @param moduleClass type of the module whose bindings should be added
   * @param configurer action to customize the module's config object
   * @param <C> the type of the module's config object
   * @param <T> the type of the module
   * @return this
   */
  <C, T extends ConfigurableModule<C>> BindingsSpec module(Class<T> moduleClass, Action<? super C> configurer);

  /**
   * Adds the bindings from the given configurable module.
   *
   * @param module module whose bindings should be added
   * @param configurer action to customize the module's config object
   * @param <C> the type of the module's config object
   * @return this
   */
  <C> BindingsSpec module(ConfigurableModule<C> module, Action<? super C> configurer);

  /**
   * Adds the bindings from the given configurable module.
   *
   * @param moduleClass type of the module whose bindings should be added
   * @param config config object for the module
   * @param configurer action to customize the module's config object
   * @param <C> the type of the module's config object
   * @param <T> the type of the module
   * @return this
   */
  <C, T extends ConfigurableModule<C>> BindingsSpec moduleConfig(Class<T> moduleClass, C config, Action<? super C> configurer);

  default <C, T extends ConfigurableModule<C>> BindingsSpec moduleConfig(Class<T> moduleClass, C config) {
    return moduleConfig(moduleClass, config, Action.noop());
  }

  /**
   * Adds the bindings from the given configurable module.
   *
   * @param module module whose bindings should be added
   * @param config config object for the module
   * @param configurer action to customize the module's config object
   * @param <C> the type of the module's config object
   * @return this
   */
  <C> BindingsSpec moduleConfig(ConfigurableModule<C> module, C config, Action<? super C> configurer);

  default <C, T extends ConfigurableModule<C>> BindingsSpec moduleConfig(T moduleClass, C config) {
    return moduleConfig(moduleClass, config, Action.noop());
  }

  /**
   * Adds bindings by directly configuring a {@link Binder}.
   *
   * @param action the binder configuration
   * @return this
   */
  BindingsSpec binder(Action<? super Binder> action);

  default <T> BindingsSpec multiBinder(TypeToken<T> type, Action<? super Multibinder<T>> action) throws Exception {
    return binder(b -> action.execute(Multibinder.newSetBinder(b, GuiceUtil.toTypeLiteral(type))));
  }

  default <T> BindingsSpec multiBinder(TypeLiteral<T> type, Action<? super Multibinder<T>> action) throws Exception {
    return multiBinder(GuiceUtil.toTypeToken(type), action);
  }

  default <T> BindingsSpec multiBinder(Class<T> type, Action<? super Multibinder<T>> action) throws Exception {
    return multiBinder(TypeCaching.typeToken(type), action);
  }

  /**
   * Add a binding for the given type.
   *
   * @param type the type to add a binding for
   * @return this
   */
  default BindingsSpec bind(Class<?> type) {
    return binder(binder -> binder.bind(type));
  }

  default <T> BindingsSpec multiBind(Class<T> type) {
    return uncheck(() -> multiBinder(type, b -> b.addBinding().to(type)));
  }

  /**
   * Add a binding for the given public type, to the given implementation type.
   *
   * @param publicType the public type of the binding
   * @param implType the class implementing the public type
   * @param <T> the public type of the binding
   * @return this
   */
  default <T> BindingsSpec bind(Class<T> publicType, Class<? extends T> implType) {
    return bind(TypeLiteral.get(publicType), implType);
  }

  default <T> BindingsSpec bind(TypeToken<T> publicType, Class<? extends T> implType) {
    return bind(GuiceUtil.toTypeLiteral(publicType), implType);
  }

  default <T> BindingsSpec bind(TypeLiteral<T> publicType, Class<? extends T> implType) {
    return binder(binder -> binder.bind(publicType).to(implType));
  }

  default <T> BindingsSpec multiBind(TypeLiteral<T> publicType, Class<? extends T> implType) {
    return uncheck(() -> multiBinder(publicType, b -> b.addBinding().to(implType)));
  }

  default <T> BindingsSpec multiBind(Class<T> publicType, Class<? extends T> implType) {
    return multiBind(TypeLiteral.get(publicType), implType);
  }

  default <T> BindingsSpec multiBind(TypeToken<T> publicType, Class<? extends T> implType) {
    return multiBind(GuiceUtil.toTypeLiteral(publicType), implType);
  }

  /**
   * Add a binding for the given public type, to the given implementing instance.
   *
   * @param publicType the public type of the binding
   * @param instance the instance that implements the public type
   * @param <T> the public type of the binding
   * @return this
   */
  default <T> BindingsSpec bindInstance(TypeLiteral<? super T> publicType, T instance) {
    return binder(b -> b.bind(publicType).toInstance(instance));
  }

  default <T> BindingsSpec bindInstance(TypeToken<? super T> publicType, T instance) {
    return bindInstance(GuiceUtil.toTypeLiteral(publicType), instance);
  }

  default <T> BindingsSpec bindInstance(Class<? super T> publicType, T instance) {
    return bindInstance(TypeLiteral.get(publicType), instance);
  }

  /**
   * Add a binding for the given object to its concrete type.
   *
   * @param instance the instance to bind
   * @param <T> the type of the binding
   * @return this
   */
  default <T> BindingsSpec bindInstance(T instance) {
    if (instance instanceof Class) {
      throw new IllegalArgumentException("cannot use bindInstance() with a class object (use bind(Class) instead)");
    }
    Class<T> type = Types.cast(instance.getClass());
    return binder(binder -> binder.bind(type).toInstance(instance));
  }

  default <T> BindingsSpec multiBindInstance(Class<T> publicType, T instance) {
    return uncheck(() -> multiBinder(publicType, b -> b.addBinding().toInstance(instance)));
  }

  default <T> BindingsSpec multiBindInstance(TypeLiteral<T> publicType, T instance) {
    return uncheck(() -> multiBinder(publicType, b -> b.addBinding().toInstance(instance)));
  }

  default <T> BindingsSpec multiBindInstance(TypeToken<T> publicType, T instance) {
    return uncheck(() -> multiBinder(publicType, b -> b.addBinding().toInstance(instance)));
  }

  default <T> BindingsSpec multiBindInstance(T instance) {
    if (instance instanceof Class) {
      throw new IllegalArgumentException("cannot use multiBindInstance() with a class object (use multiBind(Class) instead)");
    }
    Class<T> type = Types.cast(instance.getClass());
    return multiBindInstance(type, instance);
  }

  /**
   * Add a binding for the given public type, to the given provider.
   *
   * @param publicType the public type of the object
   * @param provider the provider for the object
   * @param <T> The public type of the object
   * @return this
   */
  default <T> BindingsSpec provider(TypeLiteral<T> publicType, Provider<? extends T> provider) {
    return binder(b -> b.bind(publicType).toProvider(provider));
  }

  default <T> BindingsSpec provider(TypeToken<T> publicType, Provider<? extends T> provider) {
    return provider(GuiceUtil.toTypeLiteral(publicType), provider);
  }

  default <T> BindingsSpec provider(Class<T> publicType, Provider<? extends T> provider) {
    return provider(TypeLiteral.get(publicType), provider);
  }

  default <T> BindingsSpec multiBindProvider(TypeLiteral<T> publicType, Provider<? extends T> provider) {
    return uncheck(() -> multiBinder(publicType, b -> b.addBinding().toProvider(provider)));
  }

  default <T> BindingsSpec multiBindProvider(TypeToken<T> publicType, Provider<? extends T> provider) {
    return multiBindProvider(GuiceUtil.toTypeLiteral(publicType), provider);
  }

  default <T> BindingsSpec multiBindProvider(Class<T> publicType, Provider<? extends T> provider) {
    return multiBindProvider(TypeLiteral.get(publicType), provider);
  }

  /**
   * Add a binding for the given public type, to the given provider type.
   *
   * @param publicType the public type of the object
   * @param providerType the type of the provider for the object
   * @param <T> The public type of the object
   * @return this
   */
  default <T> BindingsSpec providerType(TypeLiteral<T> publicType, Class<? extends Provider<? extends T>> providerType) {
    return binder(binder -> binder.bind(publicType).toProvider(providerType));
  }

  default <T> BindingsSpec providerType(Class<T> publicType, Class<? extends Provider<? extends T>> providerType) {
    return providerType(TypeLiteral.get(publicType), providerType);
  }

  default <T> BindingsSpec providerType(TypeToken<T> publicType, Class<? extends Provider<? extends T>> providerType) {
    return providerType(GuiceUtil.toTypeLiteral(publicType), providerType);
  }

  default <T> BindingsSpec multiBindProviderType(Class<T> publicType, Class<? extends Provider<? extends T>> providerType) {
    return uncheck(() -> multiBinder(publicType, b -> b.addBinding().toProvider(providerType)));
  }

  @Override
  default <O> RegistrySpec add(TypeToken<O> type, O object) {
    return multiBindInstance(type, object);
  }

  @SuppressWarnings({"Anonymous2MethodRef", "Convert2Lambda"})
  @Override
  default <O> RegistrySpec addLazy(TypeToken<O> type, Supplier<? extends O> supplier) {
    return multiBindProvider(type, new Provider<O>() {
      @Override
      public O get() {
        return supplier.get();
      }
    });
  }
}
