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

import spock.util.concurrent.PollingConditions

class InstallAppSpec extends FunctionalSpec {

  def "everything goes in the right place"() {
    given:
    file("src/ratpack/ratpack.groovy") << """
      import static ratpack.groovy.RatpackScript.ratpack

      ratpack {
        handlers {
          get("") {
            response.send "foo"
          }
        }
      }

    """
    when:
    run "installApp"

    def process = new ProcessBuilder().directory(file("build/install/test-app")).command("bin/test-app").start()
    process.consumeProcessOutput(System.out, System.err)

    then:
    new PollingConditions().within(10) {
      try {
        urlText("") == "foo"
      } catch (ConnectException ignore) {
        false
      }
    }

    cleanup:
    process?.destroy()
    process?.waitFor()
  }

  HttpURLConnection url(String path = "") {
    new URL("http://localhost:5050/$path").openConnection() as HttpURLConnection
  }

  String urlText(String path = "") {
    new URL("http://localhost:5050/$path").text
  }

}
