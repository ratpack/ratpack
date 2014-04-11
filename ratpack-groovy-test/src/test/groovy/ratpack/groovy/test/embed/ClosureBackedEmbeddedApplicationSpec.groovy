/*
 * Copyright 2014 the original author or authors.
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

package ratpack.groovy.test.embed

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ratpack.test.embed.BaseDirBuilder
import ratpack.test.embed.PathBaseDirBuilder
import spock.lang.AutoCleanup
import spock.lang.Specification

import static ratpack.groovy.test.TestHttpClients.testHttpClient
import static ratpack.groovy.test.embed.EmbeddedApplications.embeddedApp

class ClosureBackedEmbeddedApplicationSpec extends Specification {

  @Rule
  TemporaryFolder folder = new TemporaryFolder()

  @AutoCleanup
  BaseDirBuilder baseDir

  def setup() {
    baseDir = new PathBaseDirBuilder(folder.newFolder("app"))
  }

  def "embedded app with base dir"() {
    given:
    def myapp = embeddedApp(baseDir) {
        handlers {
          handler {
            render "foo"
          }
        }
    }

    when:
    myapp.server.start()

    then:
    testHttpClient(myapp).getText() == "foo"
  }

  def "embedded app without base dir"() {
    given:
    def myapp = embeddedApp {
      handlers {
        handler {
          render "foo"
        }
      }
    }

    when:
    myapp.server.start()

    then:
    testHttpClient(myapp).getText() == "foo"
  }

  def "asset serving embedded app without base dir"() {
    given:
    baseDir.file("public/static.text", "hello!")
    and:
    def myapp = embeddedApp {
      handlers {
        assets("public")
      }
    }

    when:
    myapp.server.start()

    then:
    testHttpClient(myapp).get("static.text").statusCode == 500
  }

}
