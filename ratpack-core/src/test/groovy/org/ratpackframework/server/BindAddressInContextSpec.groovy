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

package org.ratpackframework.server

import org.ratpackframework.test.internal.RatpackGroovyDslSpec

class BindAddressInContextSpec extends RatpackGroovyDslSpec {

  def "bind address is available via context"() {
    when:
    app {
      handlers {
        get("port") {
          response.send bindAddress.port.toString()
        }
        get("host") {
          response.send bindAddress.host
        }
      }
    }

    then:
    getText("port") == server.bindPort.toString()
    getText("host") == server.bindHost
  }

}
