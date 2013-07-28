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

package org.ratpackframework.test

import com.google.inject.Module
import org.ratpackframework.server.DefaultRatpackServerSettings
import org.ratpackframework.server.RatpackServer
import org.ratpackframework.server.RatpackServerBuilder
import org.ratpackframework.guice.internal.GuiceBackedHandlerFactory
import org.ratpackframework.guice.ModuleRegistry
import org.ratpackframework.guice.internal.DefaultGuiceBackedHandlerFactory
import org.ratpackframework.handling.Handler
import org.ratpackframework.handling.Handlers
import org.ratpackframework.handling.Chain
import org.ratpackframework.util.Action

import static Handlers.chain
import static org.ratpackframework.groovy.util.Closures.action
import static org.ratpackframework.groovy.util.Closures.configureDelegateFirst

abstract class DefaultRatpackSpec extends InternalRatpackSpec {

  Closure<?> handlersClosure = {}
  Closure<?> modulesClosure = {}

  List<Module> modules = []

  void handlers(@DelegatesTo(Chain) Closure<?> configurer) {
    this.handlersClosure = configurer
  }

  void modules(@DelegatesTo(ModuleRegistry) Closure<?> configurer) {
    this.modulesClosure = configurer
  }

  @Override
  protected RatpackServer createServer() {
    GuiceBackedHandlerFactory appFactory = createAppFactory()
    def handler = createHandler()
    def modulesAction = createModulesAction()
    Handler appHandler = appFactory.create(modulesAction, handler)

    RatpackServerBuilder builder = new RatpackServerBuilder(createServerSettings(), appHandler)
    builder.port = 0
    builder.address = null
    builder.build()
  }

  protected GuiceBackedHandlerFactory createAppFactory() {
    new DefaultGuiceBackedHandlerFactory(createServerSettings())
  }

  protected Action<? super ModuleRegistry> createModulesAction() {
    action(ModuleRegistry) { ModuleRegistry registry ->
      this.modules.each {
        registry.register(it)
      }
      configureDelegateFirst(registry, modulesClosure)
    }
  }

  protected Handler createHandler() {
    chain(action(handlersClosure))
  }

}
