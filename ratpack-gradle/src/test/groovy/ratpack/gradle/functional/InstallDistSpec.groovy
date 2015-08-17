/*
 * Copyright 2012 the original author or authors.
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

import com.google.common.base.StandardSystemProperty
import spock.util.concurrent.PollingConditions

class InstallDistSpec extends FunctionalSpec {

  def "everything goes in the right place"() {
    given:
    file("src/ratpack/ratpack.groovy") << """
      import static ratpack.groovy.Groovy.*
      import ratpack.server.Stopper

      ratpack {
        serverConfig { port 0 }
        handlers {
          get {
            onClose { get(Stopper).stop() }
            render "foo"
          }
          get("stop") {
            onClose { get(ratpack.server.Stopper).stop() }
          }
        }
      }

    """
    when:
    run "installDist"

    def process = new ProcessBuilder()
      .directory(file("build/install/test-app"))
      .command(osSpecificCommand())
      .start()
    def port = scrapePort(process)

    then:
    new PollingConditions().within(10) {
      try {
        urlText(port) == "foo"
      } catch (ConnectException ignore) {
        false
      }
    }

    cleanup:
    if (port) {
      url(port, "stop")
    }
    process?.destroy()
    process?.waitFor()
  }

  HttpURLConnection url(int port, String path = "") {
    new URL("http://localhost:$port/$path").openConnection() as HttpURLConnection
  }

  String urlText(int port, String path = "") {
    new URL("http://localhost:$port/$path").text
  }

  String osSpecificCommand() {
    if (StandardSystemProperty.OS_NAME.value().startsWith("Windows")) {
      // Windows doesn't take the working directory into account when searching for the command so a relative path won't work.
      file("build/install/test-app/bin/test-app.bat").absolutePath
    } else {
      "bin/test-app"
    }
  }

}
