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

import org.gradle.testkit.runner.BuildResult
import spock.lang.Unroll
import spock.util.concurrent.BlockingVariable

class ConfigCachingSpec extends FunctionalSpec {

  public static final ArrayList<String> TESTED_VERSIONS = [
    "6.8"
  ]
  int port


  AsyncServerRunningBuild build

  def setup() {
    uniqueDaemon = true
  }

  @Unroll
  def "can use continuous build #gradleVersion"() {
    given:
    this.gradleVersion = gradleVersion
    buildFile << """
      def stopFile = file("stop")
      tasks.all {
        onlyIf { !stopFile.exists() }
      }
    """
    file("src/ratpack/public/foo.txt") << "original"
    file("src/ratpack/.ratpack") << ""

    file("src/ratpack/ratpack.groovy") << """
      import static ratpack.groovy.Groovy.*
      import ratpack.server.Stopper
      import ratpack.server.RatpackServer
      import java.nio.file.Paths

      ratpack {
        serverConfig {
          port 0
          portFile Paths.get("port")
        }
        handlers {
          all { println "received \$context.request.uri"
            next()
           }
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
    startAsync("run", "--configuration-cache", "-t", "-s")

    then:
    urlText("foo.txt") == "original"

    when:
    file("src/ratpack/public/foo.txt").text = "changed"
    determinePort()

    then:
    polling.within(30) {
      urlText("foo.txt") == "changed"
    }

    when:
    build.stop()
    file("src/ratpack/public/foo.txt").text = "changed"
    startAsync("run", "--configuration-cache", "-t", "-s")

    then:
    urlText("foo.txt") == "changed"

    when:
    file("src/ratpack/public/foo.txt").text = "changed-again"
    determinePort()

    then:
    polling.within(10) {
      urlText("foo.txt") == "changed-again"
    }

    where:
    gradleVersion << TESTED_VERSIONS
  }

  def "can build dist #gradleVersion"() {
    this.gradleVersion = gradleVersion

    given:
    file("src/ratpack/ratpack.properties") << "foo\n"

    when:
    run "distZip", "--configuration-cache"
    run "clean"
    run "distZip", "--configuration-cache"

    then:
    unzip(distZip, file("unpacked"))
    file("unpacked/test-app-1.0/app/ratpack.properties").text.contains("foo")

    where:
    gradleVersion << TESTED_VERSIONS
  }

  void startAsync(String... args) {
    build = new AsyncServerRunningBuild()
    build.start(args)
  }

  def cleanup() {
    build?.stop()
  }

  String urlText(String path = "") {
    new URL("http://localhost:$port/$path").text
  }


  class AsyncServerRunningBuild {
    def resultHolder = new BlockingVariable(30)

    void start(String... args) {
      file("stop").delete()
      file("port").delete()
      Thread.start {
        try {
          resultHolder.set(run(args))
        } catch (ignore) {
          resultHolder.set(null)
        }
      }
      determinePort()
    }


    BuildResult stop() {
      file("stop").createNewFile()
      file("src/ratpack/public/foo.txt").text = "stop"
      if (port) {
        assert urlText() == "stopping" // stop
      }
      resultHolder.get()
    }
  }

  private void determinePort() {
    def portFile = file("port")
    polling.within(30) {
      assert portFile.isFile() && portFile.text
    }
    port = portFile.text.toInteger()
    portFile.delete()
  }

}
