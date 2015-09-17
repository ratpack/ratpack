/*
 * Copyright 2015 the original author or authors.
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

import ratpack.server.RatpackServer
import ratpack.test.embed.EmbeddedApp
import ratpack.test.embed.internal.EmbeddedAppSupport
import ratpack.test.internal.EmbeddedRatpackSpec

class RuntimeScriptEnvSpec extends EmbeddedRatpackSpec {

  EmbeddedApp application

  String script

  void script(String script) {
    this.script += script
  }

  def setup() {
    application = new EmbeddedAppSupport() {
      @Override
      RatpackServer createServer() {
        new ScriptBackedServer({
          def shell = new GroovyShell(getClass().classLoader)
          def script = shell.parse(script)
          script.run()
        })
      }
    }
  }

  def "can generate script at runtime"() {
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
