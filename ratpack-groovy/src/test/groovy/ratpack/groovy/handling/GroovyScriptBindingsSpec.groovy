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

package ratpack.groovy.handling

import ratpack.groovy.Groovy
import ratpack.server.RatpackServer
import ratpack.server.ServerConfig
import ratpack.test.embed.EmbeddedApp
import ratpack.test.embed.internal.EmbeddedAppSupport
import ratpack.test.internal.RatpackGroovyScriptAppSpec

class GroovyScriptBindingsSpec extends RatpackGroovyScriptAppSpec {

  boolean compileStatic = false
  boolean development = false

  @Override
  EmbeddedApp createApplication() {
    new EmbeddedAppSupport() {
      @Override
      protected RatpackServer createServer() {
        RatpackServer.of { spec ->
          spec
            .serverConfig(ServerConfig.builder().baseDir(temporaryFolder.root.canonicalFile).port(0).development(development))
            .registry(Groovy.Script.bindings(compileStatic, ratpackFile.name))
            .handlers { it.get { it.render(it.get(String)) } }
        }
      }
    }
  }

  def "can use script bindings"() {
    given:
    compileStatic = true

    when:
    script """
      ratpack {
        bindings {
          bindInstance(String, "foo")
        }
      }
    """

    then:
    text == "foo"
  }

  def "error in script prevents server from starting"() {
    when:
    script """
      throw new Error("Error")
    """

    server.start()

    then:
    thrown Error
  }

  def "fails to start when attempt to use handlers method and not development"() {
    given:
    compileStatic = true
    development = false

    when:
    script """
      ratpack {
        handlers {

        }
      }
    """

    server.start()

    then:
    def e = thrown IllegalStateException
    e.message == "handlers {} not supported for this script"
  }

  def "changes to bindings are respected during development"() {
    given:
    compileStatic = true
    development = true

    when:
    script """
      ratpack {
        bindings {
          bindInstance(String, "foo")
        }
      }
    """

    then:
    text == "foo"

    when:
    script """
      ratpack {
        bindings {
          bindInstance(String, "bar")
        }
      }
    """

    then:
    text == 'bar'
  }

  def "changes to bindings are not reload when not in development"() {
    given:
    compileStatic = true
    development = false

    when:
    script """
      ratpack {
        bindings {
          bindInstance(String, "foo")
        }
      }
    """

    then:
    text == "foo"

    when:
    script """
      ratpack {
        bindings {
          bindInstance(String, "bar")
        }
      }
    """

    then:
    text == 'foo'
  }

}
