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

import com.google.common.util.concurrent.AbstractIdleService
import ratpack.groovy.internal.StandaloneScriptBacking
import ratpack.groovy.internal.Util
import ratpack.groovy.launch.GroovyScriptHandlerFactory
import ratpack.launch.LaunchConfigBuilder
import ratpack.server.RatpackServer
import ratpack.server.internal.RatpackService
import ratpack.server.internal.ServiceBackedServer
import ratpack.test.internal.RatpackGroovyScriptAppSpec

class StandaloneScriptSpec extends RatpackGroovyScriptAppSpec {

  class ScriptBackedService extends AbstractIdleService implements RatpackService {
    RatpackServer server

    @Override
    protected void startUp() throws Exception {
      def shell = new GroovyShell(getClass().classLoader)
      def script = shell.parse(StandaloneScriptSpec.this.ratpackFile)

      StandaloneScriptBacking.captureNext(Util.delegatingAction {
        server = it
      })

      Thread.start {
        script.run()
      }

      def stopAt = System.currentTimeMillis() + 10000
      while (System.currentTimeMillis() < stopAt) {
        if (server != null) {
          break
        }
        sleep 100
      }

      if (!server) {
        throw new IllegalStateException("Server did not start")
      }

      server.start()
    }

    @Override
    protected void shutDown() throws Exception {
      server?.stop()
    }

    @Override
    String getScheme() {
      server.scheme
    }

    @Override
    int getBindPort() {
      server.bindPort
    }

    @Override
    String getBindHost() {
      server.bindHost
    }
  }

  @Override
  File getRatpackFile() {
    file("custom.groovy")
  }

  @Override
  RatpackServer createServer() {
    def service = new ScriptBackedService()
    new ServiceBackedServer(service, LaunchConfigBuilder.baseDir(ratpackFile.parentFile).build(new GroovyScriptHandlerFactory()))
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
}
