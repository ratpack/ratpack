/*
 * Copyright 2017 the original author or authors.
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

package ratpack.core.server

import io.netty.channel.ChannelOption
import ratpack.test.internal.RatpackGroovyDslSpec

class ServerChannelConfigurationSpec extends RatpackGroovyDslSpec {

  def "can configure so_backlog"() {
    when:
    serverConfig {
      connectQueueSize 10
    }
    handlers {
      get {
        render directChannelAccess.channel.parent().config().getOption(ChannelOption.SO_BACKLOG).toString()
      }
    }

    then:
    text == "10"
  }

  def "keep alive defaults to false"() {
    when:
    handlers {
      get {
        render directChannelAccess.channel.config().getOption(ChannelOption.SO_KEEPALIVE).toString()
      }
    }

    then:
    text == "false"
  }

  def "can configure keep alive"() {
    when:
    serverConfig {
      tcpKeepAlive true
    }
    handlers {
      get {
        render directChannelAccess.channel.config().getOption(ChannelOption.SO_KEEPALIVE).toString()
      }
    }

    then:
    text == "true"
  }

}
