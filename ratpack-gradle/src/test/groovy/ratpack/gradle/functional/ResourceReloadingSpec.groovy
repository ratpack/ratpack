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

package ratpack.gradle.functional

import spock.lang.Timeout
import spock.util.concurrent.PollingConditions

@Timeout(120)
class ResourceReloadingSpec extends FunctionalSpec {

  int port

  def "edits to base dir contents are live"() {
    given:
    def f = file("src/ratpack/public/foo.txt") << "original"
    file("src/ratpack/.ratpack") << ""

    file("src/ratpack/ratpack.groovy") << """
      import static ratpack.groovy.Groovy.*
      import ratpack.server.Stopper
      import ratpack.server.RatpackServer

      ratpack {
        serverConfig { port 0 }
        handlers {
          def server = registry.get(RatpackServer)
          Thread.start {
             sleep 1000
             new File("port").text = server.bindPort
          }
          files { dir "public" }
          get {
            onClose { server.stop() }
            render "stopping"
          }
        }
      }

    """
    when:
    Thread.start { run "run" }
    def portFile = file("port")
    new PollingConditions().within(30) {
      assert portFile.isFile() && portFile.text
    }
    port = portFile.text.toInteger()

    then:
    urlText(port, "foo.txt") == "original"

    when:
    f.text = "changed"

    then:
    urlText(port, "foo.txt") == "changed"
  }

  def cleanup() {
    if (port) {
      urlText(port) == "stopping"
    }
  }

  String urlText(int port, String path = "") {
    new URL("http://localhost:$port/$path").text
  }

}
