/*
 * Copyright 2016 the original author or authors.
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
import ratpack.test.internal.RatpackGroovyScriptAppSpec


class ScriptIncudeSpec extends RatpackGroovyScriptAppSpec {

  protected Map<String, File> additionalFiles = [:]

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

  def "can include additional files"() {
    when:
    File include = getAdditionalFile("include.groovy") << """
      import static ${Groovy.name}.ratpack
      ratpack {
        bindings {
          bindInstance Integer, 50
        }
        handlers {
          get("integer") {
            response.send get(Integer).toString()
          }
        }
      }
    """

    script """
      ratpack {
        bindings {
          bindInstance String, "foo"
        }
        include "${include.path}"
        handlers {
          get {
            response.send "\${get(String)}:\${get(Integer)}"
          }
        }
      }
    """

    then:
    text == "foo:50"

    and:
    getText("integer") == "50"
  }

  def "can include nested files"() {
    when:
    File nestedInclude = getAdditionalFile("nestedInclude.groovy") << """
      import static ${Groovy.name}.ratpack
      ratpack {
        bindings {
          bindInstance Integer, 50
        }
      }
    """

    File include = getAdditionalFile("include.groovy") << """
      import static ${Groovy.name}.ratpack
      ratpack {
        include "${nestedInclude.path}"
        handlers {
          get("integer") {
            response.send get(Integer).toString()
          }
        }
      }
    """

    script """
      ratpack {
        bindings {
          bindInstance String, "foo"
        }
        include "${include.path}"
        handlers {
          get {
            response.send "\${get(String)}:\${get(Integer)}"
          }
        }
      }
    """

    then:
    text == "foo:50"

    and:
    getText("integer") == "50"
  }

  protected File getAdditionalFile(String fileName) {
    if (!additionalFiles.containsKey(fileName)) {
      additionalFiles[fileName] = temporaryFolder.newFile(fileName)
    }
    return additionalFiles[fileName]
  }
}
