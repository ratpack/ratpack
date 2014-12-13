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

package ratpack.groovy.launch

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ratpack.launch.LaunchConfigs
import ratpack.server.RatpackServer
import spock.lang.Specification

import java.nio.file.Paths

class GroovyRatpackMainSpec extends Specification {

  @Rule
  TemporaryFolder dir = new TemporaryFolder()

  def "starts ratpack app"() {
    given:
    File ratpackFile = dir.newFile("ratpack.groovy")
    ratpackFile << """
      import static ratpack.groovy.Groovy.*
      import ratpack.server.Stopper

      ratpack {
        handlers {
          get {
            get(Stopper).stop()
            render "foo"
          }
          get("stop") {
            get(ratpack.server.Stopper).stop()
          }
        }
      }
    """

    Properties overrides = System.properties
    overrides.put("ratpack.${LaunchConfigs.CONFIG_RESOURCE_PROPERTY}".toString(), Paths.get(dir.root.absolutePath, LaunchConfigs.CONFIG_RESOURCE_DEFAULT).toAbsolutePath().toString())
    RatpackServer server = new GroovyRatpackMain().server(overrides, new Properties())

    when:
    server.start()

    then:
    server.isRunning()

    cleanup:
    if (server.isRunning()) {
      server.stop()
    }
  }
}
