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

import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.server.RatpackServerBuilder
import spock.lang.Specification

class LaunchConfigBuilderSpec extends Specification {

  static class TestHandlerFactory implements HandlerFactory {
    @Override
    Handler create(LaunchConfig launchConfig) {
      new Handler() {
        void handle(Context context) throws Exception { }
      }
    }
  }

  def "no base dir"() {
    given:
    def launchConfig = LaunchConfigBuilder.noBaseDir().build(new TestHandlerFactory())

    when:
    launchConfig.baseDir

    then:
    thrown(NoBaseDirException)

    cleanup:
    launchConfig.execController.close()
  }

  def "error subclass thrown from HandlerFactory's create method"() {
    given:
    def e = new Error("e")
    def config = LaunchConfigBuilder.noBaseDir().build(new HandlerFactory() {
      Handler create(LaunchConfig launchConfig) throws Exception {
        throw e
      }
    })
    def server = RatpackServerBuilder.build(config)

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
