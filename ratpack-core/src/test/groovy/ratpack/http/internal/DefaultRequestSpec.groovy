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

import io.netty.buffer.ByteBuf
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpVersion
import ratpack.exec.Promise
import ratpack.func.Block
import ratpack.http.Headers
import ratpack.server.ServerConfig
import ratpack.server.internal.RequestBodyReader
import ratpack.stream.TransformablePublisher
import ratpack.test.internal.RatpackGroovyDslSpec

import java.time.Instant

class DefaultRequestSpec extends RatpackGroovyDslSpec {

  def "Properly parses uri/query/path based on input #inputUri"() {
    given:
    def headers = Mock(Headers)

    when:
    def request = new DefaultRequest(
      Instant.now(),
      headers,
      HttpMethod.GET,
      HttpVersion.HTTP_1_1,
      inputUri,
      new InetSocketAddress('localhost', 45678),
      new InetSocketAddress('localhost', 5050),
      ServerConfig.builder().build(),
      new NoRequestBodyReader(),
      {},
      null
    )

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
    "http://example.com"                           | "/"                                            | ""                             | ""
    "http://example.com?message=hello"             | "/?message=hello"                              | "message=hello"                | ""
    "http://example.com:8080/?message=hello"       | "/?message=hello"                              | "message=hello"                | ""
    "http://example.com:8080/user/12345"           | "/user/12345"                                  | ""                             | "user/12345"
    "https://example.com:8443/user?name=fred"      | "/user?name=fred"                              | "name=fred"                    | "user"
  }

  def "It should detect an AJAX request"() {
    given:
    def headers = Mock(Headers)

    when:
    def request = new DefaultRequest(
      Instant.now(),
      headers,
      HttpMethod.GET,
      HttpVersion.HTTP_1_1,
      '/user/12345',
      new InetSocketAddress('localhost', 45678),
      new InetSocketAddress('localhost', 5050),
      ServerConfig.builder().build(),
      new NoRequestBodyReader(),
      {},
      null,
    )
    Boolean result = request.isAjaxRequest()

    then:
    1 * headers.get(HttpHeaderConstants.X_REQUESTED_WITH) >> requestedWithHeaderValue
    assert result == isAjaxRequest

    where:
    requestedWithHeaderValue                           | isAjaxRequest
    HttpHeaderConstants.XML_HTTP_REQUEST               | true
    HttpHeaderConstants.XML_HTTP_REQUEST.toLowerCase() | true
    null                                               | false
    ''                                                 | false
  }

  static class NoRequestBodyReader implements RequestBodyReader {

    long maxContentLength = 1024

    @Override
    long getContentLength() {
      -1
    }

    @Override
    Promise<? extends ByteBuf> read(Block onTooLarge) {
      throw new UnsupportedOperationException()
    }

    @Override
    TransformablePublisher<? extends ByteBuf> readStream() {
      throw new UnsupportedOperationException()
    }
  }
}
