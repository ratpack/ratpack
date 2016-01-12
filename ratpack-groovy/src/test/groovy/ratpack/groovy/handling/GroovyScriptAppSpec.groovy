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
import ratpack.impose.ForceServerListenPortImposition
import ratpack.impose.Impositions
import ratpack.server.RatpackServer
import ratpack.test.embed.EmbeddedApp
import ratpack.test.embed.internal.EmbeddedAppSupport
import ratpack.test.internal.RatpackGroovyScriptAppSpec

class GroovyScriptAppSpec extends RatpackGroovyScriptAppSpec {

  boolean compileStatic = false
  boolean development = false

  String[] args = [] as String[]

  @Override
  EmbeddedApp createApplication() {
    new EmbeddedAppSupport() {
      @Override
      protected RatpackServer createServer() {
        Impositions.of { it.add ForceServerListenPortImposition.ephemeral() }.impose {
          RatpackServer.of(Groovy.Script.appWithArgs(compileStatic, ratpackFile.canonicalFile.toPath(), args))
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

  def "can use Ratpack.groovy script app"() {
    given:
    compileStatic = true
    def app = new EmbeddedAppSupport() {
      @Override
      protected RatpackServer createServer() {
        RatpackServer.of(Groovy.Script.app(compileStatic))
      }
    }

    when:
    script """
      ratpack {
        serverConfig { port 0 }
        handlers {
          get {
            render "foo"
          }
        }
      }
    """
    temporaryFolder.newFolder('customFile')
    File customRatpackFile = temporaryFolder.newFile('customFile/Ratpack.groovy')
    customRatpackFile.text = ratpackFile.text
    ratpackFile.delete()
    GroovyScriptAppSpec.classLoader.addURL(customRatpackFile.parentFile.toURI().toURL())


    then:
    app.httpClient.text == "foo"

    cleanup:
    app.close()
  }

  def "dangling handlers in scripts are reported"() {
    when:
    script """
      ratpack {
        serverConfig {
          development true
        }
        handlers {
          get {
            // no response
          }
        }
      }
    """


    then:
    // line 6 because of prefix added by script() method
    def string = "No response sent for GET request to / (last handler: closure at line 9 of ${ratpackFile.getCanonicalFile().toPath().toUri()})"
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

  def "can use script bindings"() {
    given:
    compileStatic = true

    when:
    script """
      ratpack {
        bindings {
          bindInstance(String, "foo")
        }
        handlers {
          get {
            render get(String)
          }
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

  def "changes to app are respected during development"() {
    given:
    compileStatic = true

    when:
    script """
      class RequestCounter {
        int requests

        int inc() {
          return ++requests
        }
      }
      ratpack {
        serverConfig {
          development true
        }
        bindings {
          bindInstance new RequestCounter()
        }
        handlers {
          get {
            render get(RequestCounter).inc().toString()
          }
        }
      }
    """

    then:
    text == "1"
    text == "2"

    when:
    script """
      class RequestCounter {
        int requests

        int inc() {
          return ++requests
        }
      }
      ratpack {
        bindings {
          bindInstance new RequestCounter()
        }
        serverConfig {
          development true
        }
        handlers {
          get {
            render get(RequestCounter).inc().toString()
          }
        }
      }
    """

    then:
    text == "1"
    text == "2"
  }

  def "changes to app are not reloaded when not development"() {
    given:
    compileStatic = true

    when:
    script """
      ratpack {
        serverConfig {
          development false
        }
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
        serverConfig {
          development false
        }
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

  def "defining a class inside for ratpack.groovy"() {
    when:
    script """
      import ratpack.server.*
      import ratpack.groovy.Groovy.Ratpack

      import org.slf4j.*
      import javax.inject.Inject
      import groovy.util.logging.*

      final Logger log = LoggerFactory.getLogger(Ratpack)

      @Slf4j
      class DbInit implements Service {

        @Inject
        DbInit() {
        }

        void onStart(StartEvent event) throws Exception {
          log.info "Initializing DB"
        }
      }

      ratpack {
        serverConfig {
          development false
        }
        bindings {
          bind DbInit
        }
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

  def "can access scripts args via args variable"() {
    when:
    args = ["foo", "bar"] as String[]
    script """
      ratpack {
        handlers {
          get(":i") { render args[pathTokens.i.toInteger()] }
        }
      }
    """

    then:
    getText("0") == "foo"
    getText("1") == "bar"

  }
}
