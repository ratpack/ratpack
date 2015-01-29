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
import ratpack.test.embed.EmbeddedApp
import ratpack.test.embed.internal.EmbeddedAppSupport
import ratpack.test.internal.RatpackGroovyScriptAppSpec

class GroovyScriptAppSpec extends RatpackGroovyScriptAppSpec {

  boolean compileStatic = false
  boolean development = false

  @Override
  EmbeddedApp createApplication() {
    new EmbeddedAppSupport() {
      @Override
      protected RatpackServer createServer() {
        RatpackServer.of(Groovy.Script.app(ratpackFile.canonicalPath, compileStatic))
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
    ratpackFile.renameTo(new File(ratpackFile.parent, 'Ratpack.groovy'))

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

  def "dangling handlers in scripts are reported"() {
    when:
    script """
      ratpack {
        config {
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
        config {
          development true
        }
        bindings {
          bind RequestCounter
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

    when:
    ratpackFile.lastModified = System.currentTimeMillis()

    then:
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
        config {
          development true
        }
        bindings {
          bind RequestCounter
        }
        handlers {
          get {
            render get(RequestCounter).inc().toString()
          }
        }
      }
    """

    then:
    text == "3"

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
          bind RequestCounter
        }
        config {
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
  }

  def "changes to app are not reloaded when not development"() {
    given:
    compileStatic = true

    when:
    script """
      ratpack {
        config {
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
        config {
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
      class DbInit implements ServerLifecycleListener {

        @Inject
        DbInit() {
        }

        void onStart(StartEvent event) throws Exception {
          log.info "Initializing DB"
        }
      }

      ratpack {
        config {
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
}
