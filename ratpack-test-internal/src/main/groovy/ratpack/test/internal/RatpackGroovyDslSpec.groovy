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

import ratpack.groovy.guice.GroovyModuleRegistry
import ratpack.groovy.handling.GroovyChain
import ratpack.groovy.test.embed.ClosureBackedEmbeddedApplication
import ratpack.launch.LaunchConfigBuilder

abstract class RatpackGroovyDslSpec extends EmbeddedBaseDirRatpackSpec {

  @Delegate
  ClosureBackedEmbeddedApplication application

  @Override
  def setup() {
    application = createApplication()
  }

  protected ClosureBackedEmbeddedApplication createApplication() {
    new ClosureBackedEmbeddedApplication(baseDirFactory)
  }

  void handlers(@DelegatesTo(value = GroovyChain, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer) {
    application.handlers(configurer)
  }

  void modules(@DelegatesTo(value = GroovyModuleRegistry, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer) {
    application.modules(configurer)
  }

  void launchConfig(@DelegatesTo(value = LaunchConfigBuilder, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer) {
    application.launchConfig(configurer)
  }

}
