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


class ScriptIncludeSpec extends RatpackGroovyScriptAppSpec {

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
    def path = include.path.replaceAll('\\\\', '/')
    script """
      ratpack {
        bindings {
          bindInstance String, "foo"
        }
        include "${path}"
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

  def "can include additional files relative to script"() {
    when:
    getAdditionalFile("include.groovy") << """
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
        include "include.groovy"
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

    def nestedPath = nestedInclude.path.replaceAll('\\\\', '/')

    File include = getAdditionalFile("include.groovy") << """
      import static ${Groovy.name}.ratpack
      ratpack {
        include "${nestedPath}"
        handlers {
          get("integer") {
            response.send get(Integer).toString()
          }
        }
      }
    """

    def path = include.path.replaceAll('\\\\', '/')

    script """
      ratpack {
        bindings {
          bindInstance String, "foo"
        }
        include "${path}"
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

  def "can include relative nested files"() {
    when:
    getAdditionalFile("nested/include.groovy") << """
      import static ${Groovy.name}.ratpack
      ratpack {
        include "child/include.groovy"
        handlers {
          get("integer") {
            response.send get(Integer).toString()
          }
        }
      }
    """

    getAdditionalFile("nested/child/include.groovy") << """
      import static ${Groovy.name}.ratpack
      ratpack {
        bindings {
          bindInstance Integer, 50
        }
      }
    """

    script """
      ratpack {
        bindings {
          bindInstance String, "foo"
        }
        include "nested/include.groovy"
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

  def "includes overloading"() {
    when:
    File include = getAdditionalFile("include.groovy") << """
      import static ${Groovy.name}.ratpack
      ratpack {
        handlers {
          get {
            response.send "included:\${get(String)}"
          }
        }
      }
    """
    def path = include.path.replaceAll('\\\\', '/')
    script """
      ratpack {
        bindings {
          bindInstance String, "foo"
        }
        include "${path}"
        handlers {
          get {
            response.send "main:\${get(String)}"
          }
        }
      }
    """

    then:
    text == "main:foo"
  }

  def "cannot use include method from anywhere but top level"() {
    when:
    script """
      ratpack {
        handlers {
          include "foo"
        }
      }
    """
    getText()

    then:
    def e = thrown IllegalStateException
    e.message == "include {} DSL method can only be used at the top level of the ratpack {} block"
  }

  protected File getAdditionalFile(String fileName) {
    if (!additionalFiles.containsKey(fileName)) {
      def tokens = fileName.split('/')
      if (tokens.length > 1) {
        try {
          temporaryFolder.newFolder((String[]) tokens[0..-2].toArray())
        } catch (IOException e) {
          if (!e.message.contains('already exists')) {
            throw e
          }
        }

      }
      additionalFiles[fileName] = temporaryFolder.newFile(fileName)
    }
    return additionalFiles[fileName]
  }
}
