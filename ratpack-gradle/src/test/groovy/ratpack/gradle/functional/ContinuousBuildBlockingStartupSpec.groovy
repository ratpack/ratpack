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

class ContinuousBuildBlockingStartupSpec extends FunctionalSpec {

  int port
  def resultHolder = new BlockingVariable(30)

  @Unroll
  def "can use continuous build when app startup blocks"() {
    given:
    uniqueDaemon = true
    buildFile << """
      tasks.all {
        onlyIf { !file("stop").exists() }
      }

      application.mainClass.set("app.App")
    """
    file("src/ratpack/public/foo.txt") << "original"
    file("src/ratpack/.ratpack") << ""

    file("src/main/java/App.java") << """
      package app;
      import ratpack.core.server.RatpackServer;
      import java.time.Duration;
      import java.nio.file.Path;
      import java.nio.file.Paths;

      public class App {
        public static void main(String... args) throws Exception {
          RatpackServer server = RatpackServer.of(s -> {
            s.serverConfig(c -> c.findBaseDir().port(0).portFile(Paths.get("port")));
            s.handlers(c -> c
              .files(f -> f.dir("public"))
              .get(ctx -> {
                ctx.onClose(o -> ctx.get(RatpackServer.class).stop());
                ctx.render("stopping");
              })
            );
          });
          server.start();
          server.await(Duration.ofSeconds(30));
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
    determinePort()

    then:
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
  }

  void determinePort() {
    def portFile = file("port")
    new PollingConditions().within(30) {
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
