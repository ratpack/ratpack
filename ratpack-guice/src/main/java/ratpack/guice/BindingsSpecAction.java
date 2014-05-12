/*
 * Copyright 2014 the original author or authors.
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

import com.google.inject.Injector;
import com.google.inject.Module;
import ratpack.func.Action;
import ratpack.launch.LaunchConfig;

import javax.inject.Provider;

/**
 * Convenient base for {@code Action<BindingsSpec>} implementations.
 */
public abstract class BindingsSpecAction implements Action<BindingsSpec>, BindingsSpec {

  private BindingsSpec bindingsSpec;

  protected BindingsSpec getBindingsSpec() throws IllegalStateException {
    if (bindingsSpec == null) {
      throw new IllegalStateException("no bindingsSpec set - BindingSpec methods should only be called during execute()");
    }
    return bindingsSpec;
  }

  /**
   * Delegates to {@link #execute()}, using the given {@code chain} for delegation.
   *
   * @param bindingsSpec the bindings spec
   * @throws Exception any thrown by {@link #execute()}
   */
  public final void execute(BindingsSpec bindingsSpec) throws Exception {
    try {
      this.bindingsSpec = bindingsSpec;
      execute();
    } finally {
      this.bindingsSpec = bindingsSpec;
    }
  }

  /**
   * Implementations can naturally use the {@link BindingsSpec} API for the duration of this method.
   *
   * @throws Exception any
   */
  protected abstract void execute() throws Exception;

  /**
   * {@inheritDoc}
   */
  @Override
  public LaunchConfig getLaunchConfig() {
    return getBindingsSpec().getLaunchConfig();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void add(Module... modules) {
    getBindingsSpec().add(modules);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void add(Iterable<? extends Module> modules) {
    getBindingsSpec().add(modules);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T extends Module> T config(Class<T> moduleClass) throws NoSuchModuleException {
    return getBindingsSpec().config(moduleClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void bind(Class<?> type) {
    getBindingsSpec().bind(type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> void bind(Class<T> publicType, Class<? extends T> implType) {
    getBindingsSpec().bind(publicType, implType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> void bind(Class<? super T> publicType, T instance) {
    getBindingsSpec().bind(publicType, instance);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> void bind(T instance) {
    getBindingsSpec().bind(instance);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> void provider(Class<T> publicType, Class<? extends Provider<? extends T>> providerType) {
    getBindingsSpec().provider(publicType, providerType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void init(Action<Injector> action) {
    getBindingsSpec().init(action);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void init(Class<? extends Runnable> clazz) {
    getBindingsSpec().init(clazz);
  }
}
