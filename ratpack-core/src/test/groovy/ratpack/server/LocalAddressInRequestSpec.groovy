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

package ratpack.server

import ratpack.server.internal.HostUtil
import ratpack.test.internal.RatpackGroovyDslSpec

class LocalAddressInRequestSpec extends RatpackGroovyDslSpec {

  def "local address is available via request"() {
    when:
    handlers {
      get("port") {
        response.send request.localAddress.port.toString()
      }
      get("host") {
        response.send HostUtil.determineHost(request.localAddress)
      }
    }

    then:
    getText("port") == server.bindPort.toString()
    getText("host") == server.bindHost
  }

}
