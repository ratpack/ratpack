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
import ratpack.groovy.guice.GroovyBindingsSpec
import ratpack.groovy.handling.GroovyChain
import ratpack.groovy.internal.ClosureUtil
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.launch.LaunchConfigBuilder
import ratpack.test.embed.EmbeddedApp

abstract class RatpackGroovyDslSpec extends EmbeddedBaseDirRatpackSpec {

  protected final List<Module> modules = []
  protected Closure<?> _handlers = ClosureUtil.noop()
  protected Closure<?> _bindings = ClosureUtil.noop()
  protected Closure<?> _launchConfig = ClosureUtil.noop()
  protected Injector parentInjector

  @Delegate
  final EmbeddedApp application = createApplication()

  protected EmbeddedApp createApplication() {
    GroovyEmbeddedApp.build {
      if (this.baseDir) {
        baseDir(this.baseDir)
      }
      handlers(this._handlers)
      bindings {
        modules.each { add(it) }
        it.with(this._bindings)
      }
      launchConfig(this._launchConfig)
      if (this.parentInjector) {
        parentInjector(this.parentInjector)
      }
    }
  }

  void handlers(@DelegatesTo(value = GroovyChain, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer) {
    _handlers = configurer
  }

  void bindings(@DelegatesTo(value = GroovyBindingsSpec, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer) {
    _bindings = configurer
  }

  void launchConfig(@DelegatesTo(value = LaunchConfigBuilder, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer) {
    _launchConfig = configurer
  }

}
