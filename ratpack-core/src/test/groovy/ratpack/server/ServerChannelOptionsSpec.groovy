/*
 * Copyright 2023 the original author or authors.
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

import io.netty.channel.ChannelOption
import ratpack.test.internal.RatpackGroovyDslSpec

class ServerChannelOptionsSpec extends RatpackGroovyDslSpec {

    static class Conf1 implements ServerChannelOptions {

        int val = 20

        void setOptions(OptionSetter setter) {
            setter.set(ChannelOption.SO_BACKLOG, val)
        }

        @Override
        void setChildOptions(OptionSetter setter) {
            setter.set(ChannelOption.SO_RCVBUF, val)
        }
    }

    static class Conf2 implements ServerChannelOptions {

        int val = 20

        @Override
        void setOptions(OptionSetter setter) {
            setter.set(ChannelOption.SO_BACKLOG, val)
        }

    }

    def "can configure server channel options"() {
        when:
        serverConfig {
            connectQueueSize 9
            props("c1.val": "400000")
            props("c2.val": "800000")
            require("/c1", Conf1)
            require("/c2", Conf2)
        }
        handlers {
            get {
                def parent = directChannelAccess.channel.parent().config().getOption(ChannelOption.SO_BACKLOG)
                def child = directChannelAccess.channel.config().getOption(ChannelOption.SO_RCVBUF)
                render "$parent:$child"
            }
        }

        then:
        text == "800000:400000"
    }
}
