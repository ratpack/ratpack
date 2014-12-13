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

package ratpack.launch

import ratpack.handling.Handler
import ratpack.registry.Registry
import spock.lang.Specification

class ServerConfigBuilderSpec extends Specification {

  def "no base dir"() {
    given:
    ServerConfig serverConfig = ServerConfigBuilder.noBaseDir().build()

    when:
    serverConfig.baseDir

    then:
    thrown(NoBaseDirException)
  }

  def "error subclass thrown from HandlerFactory's create method"() {
    given:
    def e = new Error("e")
    def config = ServerConfigBuilder.noBaseDir().build()
    def server = RatpackLauncher.launcher({r ->
      r.add(ServerConfig, config)
    }).build(new HandlerFactory() {
      @Override
      Handler create(Registry rootRegistry) throws Exception {
        throw e
      }
    })

    when:
    server.start()

    then:
    thrown(Error)
    !server.running

    cleanup:
    if (server && server.running) {
      server.stop()
    }

  }

}
