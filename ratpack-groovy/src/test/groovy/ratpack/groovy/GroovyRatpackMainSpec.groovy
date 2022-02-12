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

import ratpack.groovy.test.GroovyRatpackMainApplicationUnderTest
import ratpack.test.MainClassApplicationUnderTest
import ratpack.test.internal.BaseRatpackSpec
import ratpack.test.internal.spock.TempDir
import ratpack.test.internal.spock.TemporaryFolder

class GroovyRatpackMainSpec extends BaseRatpackSpec {

  @TempDir
  TemporaryFolder dir

  def "starts ratpack app"() {
    given:
    def loader = new URLClassLoader([dir.root.toURI().toURL()] as URL[])
    Thread.currentThread().setContextClassLoader(loader)
    File ratpackFile = dir.newFile("ratpack.groovy")
    ratpackFile << """
      import static ratpack.groovy.Groovy.*
      import ratpack.core.server.Stopper

      ratpack {
        handlers {
          get {
            render "foo"
          }
          get("stop") {
            get(ratpack.core.server.Stopper).stop()
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
    Thread.currentThread().setContextClassLoader(GroovyRatpackMainSpec.classLoader)
    aut?.close()
  }
}
