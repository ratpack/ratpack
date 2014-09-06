/*
 * Copyright 2013 the original author or authors.
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
import org.gradle.internal.jvm.Jvm
import spock.util.concurrent.PollingConditions

class ShadowJarSpec extends FunctionalSpec {

  def "can create and run shadow jar"() {
    given:
    file("src/ratpack/public/foo.txt") << "bar"
    file("src/ratpack/ratpack.properties") << ""

    file("src/ratpack/ratpack.groovy") << """
      import static ratpack.groovy.Groovy.*
      import ratpack.server.Stopper

      ratpack {
        handlers {
          assets "public"
          get { Stopper stopper ->
            onClose { stopper.stop() }
            render "stopping"
          }
        }
      }

    """
    when:
    run "shadowJar"
    def p = new ProcessBuilder().command(Jvm.current().javaExecutable.absolutePath, "-jar", shadowJar.absolutePath).start()
    p.consumeProcessOutput(System.out as Appendable, System.err as Appendable)

    then:
    new PollingConditions().within(10) {
      try {
        urlText("foo.txt") == "bar"
        urlText() == "stopping"
      } catch (ConnectException ignore) {
        false
      }
    }

    cleanup:
    p?.destroy()
  }

  HttpURLConnection url(String path = "") {
    new URL("http://localhost:5050/$path").openConnection() as HttpURLConnection
  }

  String urlText(String path = "") {
    new URL("http://localhost:5050/$path").text
  }

  String osSpecificCommand() {
    if (StandardSystemProperty.OS_NAME.value().startsWith("Windows")) {
      // Windows doesn't take the working directory into account when searching for the command so a relative path won't work.
      return file("build/install/test-app/bin/test-app.bat").absolutePath
    } else {
      return "bin/test-app"
    }
  }
}
