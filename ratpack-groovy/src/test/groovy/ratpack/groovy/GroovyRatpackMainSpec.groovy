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

package ratpack.groovy

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import ratpack.test.MainClassApplicationUnderTest
import spock.lang.Specification

class GroovyRatpackMainSpec extends Specification {

  @Rule
  TemporaryFolder dir = new TemporaryFolder()

  def "starts ratpack app"() {
    given:
    GroovyRatpackMainSpec.classLoader.addURL(dir.root.toURI().toURL())
    File ratpackFile = dir.newFile("ratpack.groovy")
    this.class.classLoader
    ratpackFile << """
      import static ratpack.groovy.Groovy.*
      import ratpack.server.Stopper

      ratpack {
        handlers {
          get {
            render "foo"
          }
          get("stop") {
            get(ratpack.server.Stopper).stop()
          }
        }
      }
    """

    MainClassApplicationUnderTest aut = new GroovyRatpackMainApplicationUnderTest()

    when:
    String response = aut.httpClient.text

    then:
    response == "foo"

    cleanup:
    aut?.close()
  }
}
