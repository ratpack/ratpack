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

package ratpack.groovy.test

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class LocalScriptApplicationUnderTestSpec extends Specification {

  @Rule TemporaryFolder tempDir = new TemporaryFolder()

  void applicationScript(String script) {
    tempDir.newFile('ratpack.groovy') << script
  }

  private String getConfigResourceAbsolutePath() {
    tempDir.newFile('ratpack.properties').absolutePath
  }

  void 'can be used for testing script backed applications'() {
    given:
    applicationScript """
      import static ratpack.groovy.Groovy.*

      ratpack {
        handlers {
          get {
            render 'from aut'
          }
        }
      }
    """

    when:
    def aut = new LocalScriptApplicationUnderTest(
      configResource: configResourceAbsolutePath
    )
    def client = TestHttpClients.testHttpClient(aut)

    then:
    client.getText() == "from aut"

    cleanup:
    aut.stop()
  }
}
