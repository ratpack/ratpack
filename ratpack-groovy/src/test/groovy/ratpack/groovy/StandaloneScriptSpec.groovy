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

package ratpack.groovy

import ratpack.groovy.guice.GroovyModuleRegistry
import ratpack.groovy.handling.GroovyChain
import ratpack.groovy.internal.RatpackScriptBacking
import ratpack.groovy.internal.StandaloneScriptBacking
import ratpack.groovy.launch.GroovyScriptHandlerFactory
import ratpack.launch.LaunchConfig
import ratpack.launch.LaunchConfigBuilder
import ratpack.launch.LaunchConfigFactory
import ratpack.launch.LaunchException
import ratpack.server.RatpackServer
import ratpack.server.internal.RatpackService
import ratpack.server.internal.ServiceBackedServer
import ratpack.test.internal.RatpackGroovyScriptAppSpec

class StandaloneScriptSpec extends RatpackGroovyScriptAppSpec {

  class ScriptBackedService implements RatpackService {
    volatile RatpackServer nestedServer

    @Override
    void start() throws LaunchException {
      def shell = new GroovyShell(getClass().classLoader)
      def script = shell.parse(StandaloneScriptSpec.this.ratpackFile)

      StandaloneScriptBacking.captureNext { RatpackServer it ->
        nestedServer = it
      }

      Thread.start {
        RatpackScriptBacking.withBacking(new CustomScriptBacking()) {
          script.run()
        }
      }

      def stopAt = System.currentTimeMillis() + 10000
      while (System.currentTimeMillis() < stopAt && nestedServer == null || !nestedServer.running) {
        sleep 100
      }

      if (!nestedServer) {
        throw new IllegalStateException("Server did not start")
      }
    }

    @Override
    void stop() throws Exception {
      nestedServer?.stop()
    }

    @Override
    boolean isRunning() {
      nestedServer.running
    }

    @Override
    String getScheme() {
      nestedServer.scheme
    }

    @Override
    int getBindPort() {
      nestedServer.bindPort
    }

    @Override
    String getBindHost() {
      nestedServer.bindHost
    }
  }

  class CustomScriptBacking extends StandaloneScriptBacking {
    @Override
    protected Properties createProperties(File scriptFile) {
      def properties = super.createProperties(scriptFile)
      properties.setProperty(LaunchConfigFactory.Property.PORT, "0")
      properties
    }
  }

  @Override
  File getRatpackFile() {
    file("custom.groovy")
  }

  @Override
  protected LaunchConfig createLaunchConfig() {
    LaunchConfigBuilder.baseDir(ratpackFile.parentFile).build(new GroovyScriptHandlerFactory())
  }

  @Override
  RatpackServer createServer(LaunchConfig launchConfig) {
    def service = new ScriptBackedService()
    new ServiceBackedServer(service, launchConfig)
  }

  def "can execute plain script and reload"() {
    when:
    app {
      script """
        ratpack {
          handlers {
            get {
              response.send "foo"
            }
          }
        }
      """
    }

    then:
    getText() == "foo"

    when:
    script """
      ratpack {
        handlers {
          get {
            response.send "bar"
          }
        }
      }
    """

    then:
    getText() == "bar"
  }

  def "types in API are correct"() {
    when:
    script """
      ratpack {
        modules {
          assert delegate instanceof $GroovyModuleRegistry.name
        }
        handlers {
          assert delegate instanceof $GroovyChain.name
          get {
            render "ok"
          }
        }
      }
    """

    then:
    text == "ok"
  }
}
