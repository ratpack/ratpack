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

package ratpack.test

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ratpack.groovy.test.TestHttpClients
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.launch.HandlerFactory
import ratpack.launch.LaunchConfig
import spock.lang.Specification

class RatpackMainApplicationUnderTestSpec extends Specification {

  @Rule
  TemporaryFolder tempDir = new TemporaryFolder()

  void 'can be used for testing RatpackMain backed applications'() {
    given:
    File ratpackProperties = tempDir.newFile('ratpack.properties') << """
      handlerFactory=ratpack.test.TestHandlerFactory
    """

    when:
    def aut = new RatpackMainApplicationUnderTest(
      configResource: ratpackProperties.absolutePath
    )
    def client = TestHttpClients.testHttpClient(aut)

    then:
    client.getText() == 'from aut'

    cleanup:
    aut?.stop()
  }
}

@SuppressWarnings("GroovyUnusedDeclaration")
class TestHandlerFactory implements HandlerFactory {
  Handler create(LaunchConfig launchConfig) throws Exception {
    return new Handler() {
      void handle(Context context) throws Exception {
        context.response.send("from aut")
      }
    }
  }
}
