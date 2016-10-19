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

package ratpack.server

import ratpack.groovy.Groovy
import ratpack.registry.Registry
import ratpack.test.ServerBackedApplicationUnderTest
import spock.lang.Specification

class ServerRegistrySpec extends Specification {

  def "can create user registry from base registry"() {
    when:
    def server = RatpackServer.of {
      it
        .serverConfig { it.port(0) }
        .registry { Registry.single(it.get(ServerConfig)) }
        .handler {
        Groovy.groovyHandler {
          render getAll(ServerConfig).toList().size().toString()
        }
      }
    }

    new ServerBackedApplicationUnderTest() {
      @Override
      protected RatpackServer createServer() throws Exception {
        return server
      }
    }.test {
      assert it.text == "2"
    }

    then:
    true
  }

}
