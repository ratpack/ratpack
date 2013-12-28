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

import ratpack.groovy.internal.RatpackScriptBacking
import ratpack.groovy.templating.EphemeralPortScriptBacking
import ratpack.launch.LaunchConfig
import ratpack.launch.LaunchConfigBuilder
import ratpack.server.RatpackServer
import ratpack.server.internal.ServiceBackedServer
import ratpack.test.embed.EmbeddedApplication
import ratpack.test.embed.EmbeddedApplicationSupport
import ratpack.test.internal.EmbeddedRatpackSpec

class RuntimeScriptEnvSpec extends EmbeddedRatpackSpec {

  EmbeddedApplication application

  String script

  void script(String script) {
    this.script += script
  }

  def setup() {
    application = new EmbeddedApplicationSupport(temporaryFolder.root) {
      @Override
      RatpackServer createServer() {
        def service = new ScriptBackedService({
          def shell = new GroovyShell(getClass().classLoader)
          def script = shell.parse(script)
          Thread.start {
            RatpackScriptBacking.withBacking(new EphemeralPortScriptBacking()) {
              script.run()
            }
          }
        })
        new ServiceBackedServer(service, null)
      }
    }
  }

  def "can start a ratpack app via a runtime script"() {
    when:
    script """
      import static ratpack.groovy.Groovy.ratpack

      ratpack {
        handlers {
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
