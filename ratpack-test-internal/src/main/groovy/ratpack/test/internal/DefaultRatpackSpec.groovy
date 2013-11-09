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

package ratpack.test.internal

import com.google.inject.Injector
import com.google.inject.Module
import ratpack.guice.Guice
import ratpack.guice.ModuleRegistry
import ratpack.guice.internal.DefaultGuiceBackedHandlerFactory
import ratpack.guice.internal.GuiceBackedHandlerFactory
import ratpack.handling.Chain
import ratpack.handling.Handler
import ratpack.handling.Handlers
import ratpack.launch.HandlerFactory
import ratpack.launch.LaunchConfig
import ratpack.launch.LaunchConfigBuilder
import ratpack.server.RatpackServer
import ratpack.server.RatpackServerBuilder
import ratpack.util.Action
import ratpack.util.Transformer
import ratpack.util.internal.ConstantTransformer

import static Handlers.chain
import static ratpack.groovy.Util.configureDelegateFirst
import static ratpack.groovy.Util.delegatingAction

abstract class DefaultRatpackSpec extends InternalRatpackSpec {

  Closure<?> handlersClosure = {}
  Closure<?> modulesClosure = {}
  Closure<?> launchConfigClosure = {}

  List<Module> modules = []
  Map<String, String> other = [:]

  void handlers(@DelegatesTo(Chain) Closure<?> configurer) {
    this.handlersClosure = configurer
  }

  void modules(@DelegatesTo(ModuleRegistry) Closure<?> configurer) {
    this.modulesClosure = configurer
  }

  void launchConfig(@DelegatesTo(LaunchConfigBuilder) Closure<?> configurer) {
    this.launchConfigClosure = configurer
  }

  @Override
  protected RatpackServer createServer() {
    def modulesAction = createModulesAction()

    def launchConfigBuilder = LaunchConfigBuilder
      .baseDir(dir)
      .port(0)
      .reloadable(reloadable)
      .other(other)

    configureDelegateFirst(launchConfigBuilder, launchConfigClosure)

    LaunchConfig launchConfig = launchConfigBuilder.build(new HandlerFactory() {
      Handler create(LaunchConfig launchConfig) {
        createHandlerFactory(launchConfig).create(modulesAction, createInjectorFactory(), createHandlerTransformer(launchConfig))
      }
    })

    RatpackServerBuilder.build(launchConfig)
  }

  Transformer<Module, Injector> createInjectorFactory() {
    Guice.newInjectorFactory()
  }

  @SuppressWarnings("GrMethodMayBeStatic")
  protected GuiceBackedHandlerFactory createHandlerFactory(LaunchConfig launchConfig) {
    new DefaultGuiceBackedHandlerFactory(launchConfig)
  }

  protected Action<? super ModuleRegistry> createModulesAction() {
    delegatingAction(ModuleRegistry) { ModuleRegistry registry ->
      this.modules.each {
        registry.register(it)
      }
      configureDelegateFirst(registry, modulesClosure)
    }
  }

  protected Transformer<Injector, Handler> createHandlerTransformer(LaunchConfig launchConfig) {
    new ConstantTransformer(chain(launchConfig, delegatingAction(handlersClosure)))
  }

}
