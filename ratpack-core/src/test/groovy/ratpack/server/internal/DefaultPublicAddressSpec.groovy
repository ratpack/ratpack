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

import ratpack.handling.Context
import ratpack.http.Headers
import ratpack.http.Request
import ratpack.http.internal.HttpHeaderConstants
import ratpack.server.BindAddress
import spock.lang.Specification
import spock.lang.Unroll

class DefaultPublicAddressSpec extends Specification {

  @Unroll
  def "Get URL #publicURL, #scheme, #requestUri, #hostHeader, #bindHost:#bindPort -> #expected"() {
    given:
    def context = mockContext(mockRequest(requestUri, mockHostHeaders(hostHeader)), mockBindAddress(bindPort, bindHost))
    def publicAddress = new DefaultPublicAddress(publicURL ? new URI(publicURL) : null, scheme)

    expect:
    publicAddress.getAddress(context).toString() == expected

    where:
    publicURL                       | scheme  | requestUri                                    | hostHeader              | bindHost            | bindPort || expected

    "http://conf.example.com"       | "http"  | "http://request.example.com/user/12345"       | "host.example.com"      | "localhost"         | 80       || "http://conf.example.com"
    "https://conf.example.com"      | "http"  | "http://request.example.com/user/12345"       | "host.example.com"      | "localhost"         | 8080     || "https://conf.example.com"
    "https://conf.example.com:8443" | "https" | "http://request.example.com/user/12345"       | "host.example.com"      | "localhost"         | 8443     || "https://conf.example.com:8443"

    null                            | "http"  | "http://request.example.com/user/12345"       | "host.example.com"      | "localhost"         | 80       || "http://request.example.com"
    null                            | "http"  | "https://request.example.com/user/12345"      | "host.example.com"      | "localhost"         | 8080     || "https://request.example.com"
    null                            | "https" | "https://request.example.com:8443/user/12345" | "host.example.com"      | "localhost"         | 8443     || "https://request.example.com:8443"

    null                            | "http"  | "/user/12345"                                 | "host.example.com"      | "localhost"         | 8080     || "http://host.example.com"
    null                            | "https" | "/user/12345"                                 | "host.example.com"      | "localhost"         | 8443     || "https://host.example.com"
    null                            | "http"  | "/user/12345"                                 | "host.example.com:8080" | "localhost"         | 8080     || "http://host.example.com:8080"
    null                            | "https" | "/user/12345"                                 | "host.example.com:8443" | "localhost"         | 8443     || "https://host.example.com:8443"

    null                            | "http"  | "/user/12345"                                 | null                    | "localhost"         | 80       || "http://localhost:80"
    null                            | "https" | "/user/12345"                                 | null                    | "localhost"         | 8443     || "https://localhost:8443"
    null                            | "http"  | "/user/12345"                                 | null                    | "[0:0:0:0:0:0:0:1]" | 5050     || "http://[0:0:0:0:0:0:0:1]:5050"
  }

  @Unroll
  def "Absolute request URIs are supported: #uri -> #expectedUri"() {
    given:
    def bindAddress = mockBindAddress(8080, "bind.example.com")
    def publicAddress = new DefaultPublicAddress(null, "http")

    when: "The request URI is absolute"
    def absoluteContext = mockContext(mockRequest(uri, mockHostHeaders("host.example.com")), bindAddress)
    def absoluteUri = publicAddress.getAddress(absoluteContext)

    then: "The host is part of the request URI and any Host header MUST be ignored"
    and: "The protocol and port are based on the request URI"
    absoluteUri.toString() == expectedUri

    where:
    uri                                           || expectedUri
    "http://request.example.com/user/12345"       || "http://request.example.com"
    "https://request.example.com:8443/user/12345" || "https://request.example.com:8443"
  }

  @Unroll
  def "Host headers are supported: #scheme, #host -> #expectedUri"() {
    given:
    def bindAddress = mockBindAddress(8080, "bind.example.com")
    def publicAddress = new DefaultPublicAddress(null, scheme)

    when: "The request URI is not absolute and the request includes a Host header"
    def relativeContext = mockContext(mockRequest("/user/12345", mockHostHeaders(host)), bindAddress)
    def relativeUri = publicAddress.getAddress(relativeContext)

    then: "The host is determined by the Host header value"
    and: "The port is based on the Host header value (absent means default for scheme)"
    relativeUri.toString() == expectedUri

    where:
    scheme  | host                    || expectedUri
    "http"  | "host.example.com"      || "http://host.example.com"
    "https" | "host.example.com"      || "https://host.example.com"
    "http"  | "host.example.com:8080" || "http://host.example.com:8080"
    "https" | "host.example.com:8080" || "https://host.example.com:8080"
  }

  private Headers mockHostHeaders(String host) {
    def headers = Mock(Headers)
    headers.get(HttpHeaderConstants.HOST.toString()) >> host
    return headers
  }

  private Request mockRequest(String rawUri, Headers headers) {
    return Mock(Request) {
      getRawUri() >> rawUri
      getHeaders() >> headers
    }
  }

  private Context mockContext(Request request, BindAddress bindAddress) {
    return Mock(Context) {
      getRequest() >> request
      getBindAddress() >> bindAddress
    }
  }

  private BindAddress mockBindAddress(int port, String host) {
    return Mock(BindAddress) {
      getPort() >> port
      getHost() >> host
    }
  }

}
