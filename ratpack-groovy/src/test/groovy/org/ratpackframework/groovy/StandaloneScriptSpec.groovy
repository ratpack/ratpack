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

package org.ratpackframework.groovy

import com.google.common.collect.ImmutableMap
import com.google.common.util.concurrent.AbstractIdleService
import org.ratpackframework.groovy.internal.StandaloneScriptBacking
import org.ratpackframework.groovy.launch.GroovyScriptHandlerFactory
import org.ratpackframework.launch.internal.DefaultLaunchConfig
import org.ratpackframework.server.RatpackServer
import org.ratpackframework.server.internal.RatpackService
import org.ratpackframework.server.internal.ServiceBackedServer
import org.ratpackframework.test.groovy.RatpackGroovyScriptAppSpec

class StandaloneScriptSpec extends RatpackGroovyScriptAppSpec {

  class ScriptBackedService extends AbstractIdleService implements RatpackService {
    RatpackServer server

    @Override
    protected void startUp() throws Exception {
      def shell = new GroovyShell(getClass().classLoader)
      def script = shell.parse(StandaloneScriptSpec.this.ratpackFile)

      StandaloneScriptBacking.captureNext(Util.action {
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
    new ServiceBackedServer(service, new DefaultLaunchConfig(ratpackFile.parentFile, 0, null, true, 0, new URL("http://localhost"), ImmutableMap.of(), new GroovyScriptHandlerFactory()))
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
