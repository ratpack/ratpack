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

package ratpack.test

import ratpack.impose.ImpositionsSpec
import ratpack.impose.ServerConfigImposition
import ratpack.server.RatpackServer
import ratpack.server.ServerConfig
import spock.lang.Specification

class MainClassApplicationUnderTestSpec extends Specification {

  class Main {
    public static void main(String[] args) {
      RatpackServer.of {
        it.serverConfig {
          it.props(foo: "bar")
        } handlers {
          it.get { it.render(it.get(ServerConfig).rootNode.get("foo").textValue()) }
        }
      }
    }
  }

  def "can override config"() {
    when:
    def aut = new MainClassApplicationUnderTest(Main) {
      @Override
      protected void addImpositions(ImpositionsSpec impositions) {
        impositions.add(ServerConfigImposition.of { it.props(foo: "overridden") })
      }
    }

    then:
    aut.test {
      assert it.text == "overridden"
    }
  }

}
