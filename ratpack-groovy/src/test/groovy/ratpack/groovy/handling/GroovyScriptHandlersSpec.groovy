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

package ratpack.groovy.handling

import ratpack.groovy.Groovy
import ratpack.server.RatpackServer
import ratpack.server.ServerConfig
import ratpack.test.embed.EmbeddedApp
import ratpack.test.embed.internal.EmbeddedAppSupport
import ratpack.test.internal.RatpackGroovyScriptAppSpec

class GroovyScriptHandlersSpec extends RatpackGroovyScriptAppSpec {

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
            .handler(Groovy.Script.handlers(compileStatic, ratpackFile.name))
        }
      }
    }
  }

  def "can use script app"() {
    given:
    compileStatic = true

    when:
    script """
      ratpack {
        handlers {
          get {
            render "foo"
          }
        }
      }
    """

    then:
    text == "foo"
  }


  def "cannot use handlers method from anywhere but top level"() {
    when:
    script """
      ratpack {
        handlers {
          handlers { }
        }
      }
    """
    getText()

    then:
    def e = thrown IllegalStateException
    e.message == "handlers {} DSL method can only be used at the top level of the ratpack {} block"
  }

  def "changes to handlers are respected during development"() {
    given:
    compileStatic = true
    development = true

    when:
    script """
      ratpack {
        handlers {
          get {
            render "foo"
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
            render "bar"
          }
        }
      }
    """

    then:
    text == "bar"
  }

  def "changes to handlers are respected when not development"() {
    given:
    compileStatic = true
    development = false

    when:
    script """
      ratpack {
        handlers {
          get {
            render "foo"
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
            render "bar"
          }
        }
      }
    """

    then:
    text == "foo"
  }

  def "dangling handlers in scripts are reported"() {
    given:
    development = true // so error message is written to response

    when:
    script """
      ratpack {
        handlers {
          get {
            // no response
          }
        }
      }
    """


    then:
    // line 6 because of prefix added by script() method
    def string = "No response sent for GET request to / (last handler: closure at line 6 of ${ratpackFile.getCanonicalFile().toPath().toUri()})"
    text == string
  }

  def "ratpack.groovy throws Error subclass as top level statement"() {
    when:
    script """
      import static ratpack.groovy.Groovy.ratpack

      throw new Error("Error")

      ratpack {
        handlers {
          get {
            render "ok"
          }
        }
      }
    """

    then:
    !application.server.running
    !application.server.bindHost
    application.server.bindPort < 0
  }

  def "ratpack.groovy throws Error subclass in handlers block"() {
    when:
    script """
      import static ratpack.groovy.Groovy.ratpack

      ratpack {
        handlers {
          throw new Error("Error")
          get {
            render "ok"
          }
        }
      }
    """

    then:
    !application.server.running
    !application.server.bindHost
    application.server.bindPort < 0
  }

  def "fails to start when attempt to use bindings method and not development"() {
    given:
    compileStatic = true
    development = false

    when:
    script """
      ratpack {
        bindings {

        }
      }
    """

    server.start()

    then:
    def e = thrown IllegalStateException
    e.message == "bindings {} not supported for this script"
  }

  def "does not fail to start when attempt to use bindings method and in development"() {
    given:
    compileStatic = true
    development = true

    when:
    script """
      ratpack {
        bindings {

        }
        handlers {
          get { render "ok" }
        }
      }
    """

    server.start()

    then:
    noExceptionThrown()
    get()
    response.status.code == 500
    response.body.text.contains("bindings {} not supported for this script")

    when:
    script """
      ratpack {
        handlers {
          get { render "ok" }
        }
      }
    """

    then:
    noExceptionThrown()
    text == "ok"
  }

  def "respects compile static"() {

  }
}
