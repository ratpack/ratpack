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

import ratpack.groovy.handling.GroovyChain
import ratpack.guice.BindingsSpec
import ratpack.server.RatpackServer
import ratpack.test.embed.EmbeddedApp
import ratpack.test.embed.internal.EmbeddedAppSupport
import ratpack.test.internal.RatpackGroovyScriptAppSpec

class StandaloneScriptSpec extends RatpackGroovyScriptAppSpec {

  @Override
  File getRatpackFile() {
    getApplicationFile("custom.groovy")
  }

  @Override
  EmbeddedApp createApplication() {
    new EmbeddedAppSupport() {
      @Override
      protected RatpackServer createServer() {
        new ScriptBackedServer({
          def shell = new GroovyShell(getClass().classLoader)
          def script = shell.parse(getRatpackFile())
          script.run()
        })
      }
    }
  }

  def "can execute plain script and reload"() {
    when:
    script """
      ratpack {
        serverConfig { development true }
        handlers {
          get {
            response.send "foo"
          }
        }
      }
    """

    then:
    text == "foo"

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
    text == "bar"
  }

  def "types in API are correct"() {
    when:
    script """
      ratpack {
        bindings {
          assert delegate instanceof $BindingsSpec.name
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
