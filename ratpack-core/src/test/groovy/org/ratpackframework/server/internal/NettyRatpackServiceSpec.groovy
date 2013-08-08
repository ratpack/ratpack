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

package org.ratpackframework.server.internal

import com.google.common.collect.ImmutableMap
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.ratpackframework.launch.HandlerFactory
import org.ratpackframework.launch.internal.DefaultLaunchConfig
import org.ratpackframework.server.RatpackServerBuilder
import spock.lang.Specification

import java.util.concurrent.ExecutionException

class NettyRatpackServiceSpec extends Specification {

  @Rule TemporaryFolder temporaryFolder

  def "throws exception if can't bind to port"() {
    given:
    def config1 = new DefaultLaunchConfig(temporaryFolder.root, 0, null, false, 0, ImmutableMap.of(), {} as HandlerFactory)
    def server1 = RatpackServerBuilder.build(config1)
    server1.start()

    when:
    def config2 = new DefaultLaunchConfig(temporaryFolder.root, server1.bindPort, null, false, 0, ImmutableMap.of(), {} as HandlerFactory)
    def server2 = RatpackServerBuilder.build(config2)
    server2.start()

    then:
    def e = thrown(ExecutionException)
    e.cause instanceof BindException

    cleanup:
    [server1, server2].each {
      if (it && it.running) {
        it.stop()
      }
    }
  }

}
