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
import ratpack.groovy.server.internal.GroovyKitAppFactory
import ratpack.groovy.test.embed.ClosureBackedEmbeddedApplication
import ratpack.guice.GuiceBackedHandlerFactory
import ratpack.launch.LaunchConfig
import ratpack.launch.LaunchConfigBuilder

import java.nio.file.Path

abstract class RatpackGroovyAppSpec extends EmbeddedRatpackSpec {

  class GroovyAppEmbeddedApplication extends ClosureBackedEmbeddedApplication {
    GroovyAppEmbeddedApplication(Path baseDir) {
      super(baseDir)
    }

    @Override
    protected GuiceBackedHandlerFactory createHandlerFactory(LaunchConfig launchConfig) {
      return new GroovyKitAppFactory(launchConfig)
    }
  }

  @Delegate
  ClosureBackedEmbeddedApplication application

  @Override
  def setup() {
    application = new GroovyAppEmbeddedApplication(temporaryFolder.newFolder("app").toPath())
  }

  public void handlers(@DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer) {
    application.handlers(configurer)
  }

  public void modules(@DelegatesTo(value = GroovyModuleRegistry.class, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer) {
    application.modules(configurer)
  }

  public void launchConfig(@DelegatesTo(value = LaunchConfigBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer) {
    application.launchConfig(configurer)
  }

}
