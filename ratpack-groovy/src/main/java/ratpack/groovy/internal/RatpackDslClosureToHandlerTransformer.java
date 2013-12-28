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

package ratpack.groovy.internal;

import com.google.inject.Injector;
import com.google.inject.Module;
import groovy.lang.Closure;
import ratpack.groovy.Groovy;
import ratpack.groovy.guice.internal.DefaultGroovyModuleRegistry;
import ratpack.guice.ModuleRegistry;
import ratpack.guice.GuiceBackedHandlerFactory;
import ratpack.handling.Handler;
import ratpack.launch.LaunchConfig;
import ratpack.util.Action;
import ratpack.util.Transformer;

public class RatpackDslClosureToHandlerTransformer implements Transformer<Closure<?>, Handler> {

  private final LaunchConfig launchConfig;
  private final GuiceBackedHandlerFactory handlerFactory;
  private final Transformer<? super Module, ? extends Injector> moduleTransformer;

  public RatpackDslClosureToHandlerTransformer(LaunchConfig launchConfig, GuiceBackedHandlerFactory handlerFactory, Transformer<? super Module, ? extends Injector> moduleTransformer) {
    this.launchConfig = launchConfig;
    this.handlerFactory = handlerFactory;
    this.moduleTransformer = moduleTransformer;
  }

  @Override
  public Handler transform(Closure<?> closure) throws Exception {
    final RatpackImpl ratpack = new RatpackImpl();
    ClosureUtil.configureDelegateFirst(ratpack, closure);

    final Closure<?> modulesConfigurer = ratpack.modulesConfigurer;
    Closure<?> handlersConfigurer = ratpack.handlersConfigurer;

    Action<ModuleRegistry> modulesAction = new Action<ModuleRegistry>() {
      @Override
      public void execute(ModuleRegistry thing) throws Exception {
        ClosureUtil.delegatingAction(modulesConfigurer).execute(new DefaultGroovyModuleRegistry(thing));
      }
    };


    return handlerFactory.create(modulesAction, moduleTransformer, new InjectorHandlerTransformer(launchConfig, handlersConfigurer));
  }

  static class RatpackImpl implements Groovy.Ratpack {

    private Closure<?> modulesConfigurer;
    private Closure<?> handlersConfigurer;

    public void modules(Closure<?> modulesConfigurer) {
      this.modulesConfigurer = modulesConfigurer;
    }

    public void handlers(Closure<?> handlersConfigurer) {
      this.handlersConfigurer = handlersConfigurer;
    }

  }
}
