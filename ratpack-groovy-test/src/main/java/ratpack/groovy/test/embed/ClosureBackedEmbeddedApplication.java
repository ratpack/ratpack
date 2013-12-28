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

package ratpack.groovy.test.embed;

import com.google.inject.Injector;
import com.google.inject.Module;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.groovy.Groovy;
import ratpack.groovy.guice.GroovyModuleRegistry;
import ratpack.groovy.guice.internal.DefaultGroovyModuleRegistry;
import ratpack.groovy.handling.GroovyChain;
import ratpack.groovy.internal.ClosureUtil;
import ratpack.guice.Guice;
import ratpack.guice.ModuleRegistry;
import ratpack.guice.internal.DefaultGuiceBackedHandlerFactory;
import ratpack.guice.internal.GuiceBackedHandlerFactory;
import ratpack.guice.internal.InjectorBackedRegistry;
import ratpack.handling.Handler;
import ratpack.launch.HandlerFactory;
import ratpack.launch.LaunchConfig;
import ratpack.launch.LaunchConfigBuilder;
import ratpack.test.embed.LaunchConfigEmbeddedApplication;
import ratpack.util.Action;
import ratpack.util.Transformer;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static ratpack.groovy.internal.ClosureUtil.configureDelegateFirst;

public class ClosureBackedEmbeddedApplication extends LaunchConfigEmbeddedApplication {

  protected Closure<?> handlersClosure = ClosureUtil.noop();
  protected Closure<?> modulesClosure = ClosureUtil.noop();
  protected Closure<?> launchConfigClosure = ClosureUtil.noop();

  private final List<Module> modules = new LinkedList<>();
  private final Map<String, String> other = new LinkedHashMap<>();

  public ClosureBackedEmbeddedApplication(File baseDir) {
    super(baseDir);
  }

  public List<Module> getModules() {
    return modules;
  }

  public Map<String, String> getOther() {
    return other;
  }

  @Override
  protected LaunchConfig createLaunchConfig() {
    final Action<? super ModuleRegistry> modulesAction = createModulesAction();

    LaunchConfigBuilder launchConfigBuilder = LaunchConfigBuilder
      .baseDir(getBaseDir())
      .port(0)
      .other(other);

    configureDelegateFirst(launchConfigBuilder, launchConfigClosure);

    return launchConfigBuilder.build(new HandlerFactory() {
      public Handler create(LaunchConfig launchConfig) throws Exception {
        GuiceBackedHandlerFactory handlerFactory = createHandlerFactory(launchConfig);
        Transformer<? super Module, ? extends Injector> injectorFactory = createInjectorFactory(launchConfig);
        Transformer<? super Injector, ? extends Handler> handlerTransformer = createHandlerTransformer(launchConfig);

        return handlerFactory.create(modulesAction, injectorFactory, handlerTransformer);
      }
    });
  }

  public void handlers(@DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer) {
    this.handlersClosure = configurer;
  }

  public void modules(@DelegatesTo(value = GroovyModuleRegistry.class, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer) {
    this.modulesClosure = configurer;
  }

  public void launchConfig(@DelegatesTo(value = LaunchConfigBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer) {
    this.launchConfigClosure = configurer;
  }

  public void app(@DelegatesTo(ClosureBackedEmbeddedApplication.class) Closure<?> closure) {
    ClosureUtil.configureDelegateFirst(this, closure);
  }

  protected Transformer<? super Module, ? extends Injector> createInjectorFactory(LaunchConfig launchConfig) {
    return Guice.newInjectorFactory(launchConfig);
  }

  protected GuiceBackedHandlerFactory createHandlerFactory(LaunchConfig launchConfig) {
    return new DefaultGuiceBackedHandlerFactory(launchConfig);
  }

  protected Action<? super ModuleRegistry> createModulesAction() {
    return new Action<ModuleRegistry>() {
      @Override
      public void execute(ModuleRegistry moduleRegistry) throws Exception {
        for (Module module : modules) {
          moduleRegistry.register(module);
        }
        configureDelegateFirst(new DefaultGroovyModuleRegistry(moduleRegistry), modulesClosure);
      }
    };
  }

  protected Transformer<? super Injector, ? extends Handler> createHandlerTransformer(final LaunchConfig launchConfig) throws Exception {
    return new Transformer<Injector, Handler>() {
      @Override
      public Handler transform(Injector injector) throws Exception {
        return Groovy.chain(launchConfig, new InjectorBackedRegistry(injector), handlersClosure);
      }
    };
  }
}
