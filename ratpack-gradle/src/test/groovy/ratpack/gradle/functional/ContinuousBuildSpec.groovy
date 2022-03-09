/*
 * Copyright 2017 the original author or authors.
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

import spock.lang.Unroll
import spock.util.concurrent.BlockingVariable
import spock.util.concurrent.PollingConditions

class ContinuousBuildSpec extends FunctionalSpec {

  int port
  def resultHolder = new BlockingVariable(30)

  @Unroll
  def "can use continuous build #gradleVersion"() {
    given:
    uniqueDaemon = true
    this.gradleVersion = gradleVersion
    buildFile << """
      tasks.all {
        onlyIf { !file("stop").exists() }
      }
    """
    file("src/ratpack/public/foo.txt") << "original"
    file("src/ratpack/.ratpack") << ""

    file("src/ratpack/ratpack.groovy") << """
      import static ratpack.groovy.Groovy.*
      import ratpack.core.server.Stopper
      import ratpack.core.server.RatpackServer
      import java.nio.file.Paths

      ratpack {
        serverConfig { portFile(Paths.get("port")) }
        handlers {
          files { dir "public" }
          get {
            onClose { context.get(RatpackServer).stop() }
            render "stopping"
          }
        }
      }

    """
    when:
    run("classes") // download dependencies outside of timeout

    Thread.start {
      try {
        resultHolder.set(run("run", "-t", "-S"))
      } catch (ignore) {
        resultHolder.set(null)
      }
    }

    then:
    determinePort()
    urlText("foo.txt") == "original"

    when:
    sleep 500
    file("src/ratpack/public/foo.txt").text = "changed"

    then:
    determinePort()
    new PollingConditions().within(10) {
      try {
        assert urlText("foo.txt") == "changed"
      } catch (Exception e) {
        e.printStackTrace()
        assert e == null
      }
    }

    where:
    gradleVersion << [
      "7.4"
    ]
  }

  void determinePort() {
    def portFile = file("port")
    new PollingConditions().within(45) {
      assert portFile.isFile() && portFile.text
    }
    port = portFile.text.toInteger()
    portFile.delete()
  }

  def cleanup() {
    file("stop").createNewFile()
    file("src/ratpack/public/foo.txt").text = "changed-again"

    if (port) {
      assert urlText() == "stopping"
    }
    resultHolder.get()
  }

  String urlText(String path = "") {
    new URL("http://localhost:$port/$path").text
  }

}
