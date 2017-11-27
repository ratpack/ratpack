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

package ratpack.http

import io.netty.handler.codec.http.HttpResponseStatus
import ratpack.test.internal.RatpackGroovyDslSpec

class NoBodyResponseServerSpec extends RatpackGroovyDslSpec {

  def "no content length is sent for #status response"() {
    when:
    handlers {
      get {
        response.status(status.code()).send()
      }
    }

    then:
    rawResponse() == """HTTP/1.1 ${status}
connection: close

"""

    where:
    status << noBodyResponseStatuses()
  }

  static List<HttpResponseStatus> noBodyResponseStatuses() {
    [HttpResponseStatus.valueOf(100), HttpResponseStatus.valueOf(150), HttpResponseStatus.valueOf(199), HttpResponseStatus.valueOf(204), HttpResponseStatus.valueOf(304)]
  }

}
