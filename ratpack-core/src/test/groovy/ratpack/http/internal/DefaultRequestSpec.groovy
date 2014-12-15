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

package ratpack.http.internal

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.HttpMethod
import ratpack.http.Headers
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.Unroll

class DefaultRequestSpec extends RatpackGroovyDslSpec {

  @Unroll
  def "Properly parses uri/query/path based on input #inputUri"() {
    given:
    def headers = Mock(Headers)
    def content = Unpooled.buffer()

    when:
    def request = new DefaultRequest(headers, HttpMethod.GET, inputUri, new InetSocketAddress('localhost', 45678), new InetSocketAddress('localhost', 5050), content)

    then:
    request.rawUri == inputUri
    request.uri == expectedUri
    request.query == expectedQuery
    request.path == expectedPath
    request.remoteAddress.hostText == 'localhost'
    request.remoteAddress.port == 45678
    request.localAddress.hostText == 'localhost'
    request.localAddress.port == 5050

    where:
    inputUri                                       | expectedUri                                    | expectedQuery                  | expectedPath
    "/user/12345"                                  | "/user/12345"                                  | ""                             | "user/12345"
    "/user?name=fred"                              | "/user?name=fred"                              | "name=fred"                    | "user"
    "/article/search?text=gradle&max=25&offset=50" | "/article/search?text=gradle&max=25&offset=50" | "text=gradle&max=25&offset=50" | "article/search"
    "http://example.com" | "/" | "" | ""
    "http://example.com?message=hello" | "/?message=hello" | "message=hello" | ""
    "http://example.com:8080/?message=hello" | "/?message=hello" | "message=hello" | ""
    "http://example.com:8080/user/12345" | "/user/12345" | "" | "user/12345"
    "https://example.com:8443/user?name=fred" | "/user?name=fred" | "name=fred" | "user"
  }

}
