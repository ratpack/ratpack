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

package ratpack.server.internal

import com.google.common.base.StandardSystemProperty
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ratpack.server.RatpackServer
import ratpack.server.ServerConfigBuilder
import spock.lang.IgnoreIf
import spock.lang.Specification

class NettyRatpackServiceSpec extends Specification {

  @Rule
  TemporaryFolder temporaryFolder

  @IgnoreIf({ StandardSystemProperty.OS_NAME.value().startsWith("Windows") })
  // Windows allows multiple binds (implicit SO_REUSEPORT)
  def "throws exception if can't bind to port"() {
    given:
    def config1 = ServerConfigBuilder.baseDir(temporaryFolder.root).port(0).build()
    def server1 = RatpackServer.of().config(config1).build {}
    server1.start()

    when:
    def config2 = ServerConfigBuilder.baseDir(temporaryFolder.root).port(server1.bindPort).build()
    def server2 = RatpackServer.of().config(config2).build {}
    server2.start()

    then:
    thrown BindException

    cleanup:
    [server1, server2].each {
      if (it && it.running) {
        it.stop()
      }
    }
  }

}
