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

import io.netty.handler.codec.http.DefaultHttpHeaders
import ratpack.handling.Context
import ratpack.http.Headers
import ratpack.http.Request
import ratpack.http.internal.NettyHeadersBackedMutableHeaders
import ratpack.server.BindAddress
import spock.lang.Specification
import spock.lang.Unroll

import static ratpack.http.internal.HttpHeaderConstants.*
import static ratpack.util.internal.ProtocolUtil.HTTPS_SCHEME
import static ratpack.util.internal.ProtocolUtil.HTTP_SCHEME

class DefaultPublicAddressSpec extends Specification {

  @Unroll
  def "Get URL #publicURL, #scheme, #requestUri, #headers, #bindHost:#bindPort -> #expected"() {
    given:
    def context = mockContext(mockRequest(requestUri, mockHeaders(headers)), mockBindAddress(bindPort, bindHost))
    def publicAddress = new DefaultPublicAddress(publicURL ? new URI(publicURL) : null, scheme)

    expect:
    publicAddress.getAddress(context).toString() == expected

    where:
    publicURL                       | scheme  | requestUri                                    | headers                                                  | bindHost            | bindPort || expected

    "http://conf.example.com"       | "http"  | "http://request.example.com/user/12345"       | [(HOST):"host.example.com"]                              | "localhost"         | 80       || "http://conf.example.com"
    "https://conf.example.com"      | "http"  | "http://request.example.com/user/12345"       | [(HOST):"host.example.com"]                              | "localhost"         | 8080     || "https://conf.example.com"
    "https://conf.example.com:8443" | "https" | "http://request.example.com/user/12345"       | [(HOST):"host.example.com"]                              | "localhost"         | 8443     || "https://conf.example.com:8443"

    null                            | "http"  | "http://request.example.com/user/12345"       | [(HOST):"host.example.com"]                              | "localhost"         | 80       || "http://request.example.com"
    null                            | "http"  | "https://request.example.com/user/12345"      | [(HOST):"host.example.com"]                              | "localhost"         | 8080     || "https://request.example.com"
    null                            | "https" | "https://request.example.com:8443/user/12345" | [(HOST):"host.example.com"]                              | "localhost"         | 8443     || "https://request.example.com:8443"

    null                            | "http"  | "/user/12345"                                 | [(HOST):"host.example.com", (X_FORWARDED_PROTO):"https"] | "localhost"         | 8080     || "https://host.example.com"
    null                            | "http"  | "/user/12345"                                 | [(HOST):"host.example.com", (X_FORWARDED_SSL):"on"]      | "localhost"         | 8080     || "https://host.example.com"

    null                            | "http"  | "/user/12345"                                 | [(HOST):"host.example.com"]                              | "localhost"         | 8080     || "http://host.example.com"
    null                            | "https" | "/user/12345"                                 | [(HOST):"host.example.com"]                              | "localhost"         | 8443     || "https://host.example.com"
    null                            | "http"  | "/user/12345"                                 | [(HOST):"host.example.com:8080"]                         | "localhost"         | 8080     || "http://host.example.com:8080"
    null                            | "https" | "/user/12345"                                 | [(HOST):"host.example.com:8443"]                         | "localhost"         | 8443     || "https://host.example.com:8443"

    null                            | "http"  | "/user/12345"                                 | [:]                                                      | "localhost"         | 80       || "http://localhost"
    null                            | "http"  | "/user/12345"                                 | [:]                                                      | "localhost"         | 8080     || "http://localhost:8080"
    null                            | "https" | "/user/12345"                                 | [:]                                                      | "localhost"         | 443      || "https://localhost"
    null                            | "https" | "/user/12345"                                 | [:]                                                      | "localhost"         | 8443     || "https://localhost:8443"
    null                            | "http"  | "/user/12345"                                 | [:]                                                      | "[0:0:0:0:0:0:0:1]" | 5050     || "http://[0:0:0:0:0:0:0:1]:5050"
  }

  @Unroll
  def "Absolute request URIs are supported: #uri -> #expectedUri"() {
    given:
    def bindAddress = mockBindAddress(8080, "bind.example.com")
    def publicAddress = new DefaultPublicAddress(null, "http")

    when: "The request URI is absolute"
    def context = mockContext(mockRequest(uri, mockHostHeaders("host.example.com")), bindAddress)
    def address = publicAddress.getAddress(context)

    then: "The host is part of the request URI and any Host header MUST be ignored"
    and: "The protocol and port are based on the request URI"
    address.toString() == expectedUri

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
    def context = mockContext(mockRequest("/user/12345", mockHostHeaders(host)), bindAddress)
    def address = publicAddress.getAddress(context)

    then: "The host is determined by the Host header value"
    and: "The port is based on the Host header value (absent means default for scheme)"
    address.toString() == expectedUri

    where:
    scheme  | host                    || expectedUri
    "http"  | "host.example.com"      || "http://host.example.com"
    "https" | "host.example.com"      || "https://host.example.com"
    "http"  | "host.example.com:8080" || "http://host.example.com:8080"
    "https" | "host.example.com:8080" || "https://host.example.com:8080"
  }

  def "X-Forwarded-Proto headers are supported"() {
    given:
    def bindAddress = mockBindAddress(8081, "bind.example.com")
    def publicAddress = new DefaultPublicAddress(null, HTTP_SCHEME)

    when: "The request includes a X-Forwarded-Proto header and no public URL is defined"
    def context = mockContext(mockRequest(uri, mockHeaders([(HOST):host, (X_FORWARDED_PROTO): HTTPS_SCHEME])), bindAddress)
    def address = publicAddress.getAddress(context)

    then: "The protocol is based on the header value in preference to absolute request URI or service scheme"
    address.toString() == expectedUri

    where:
    uri                                          | host                    || expectedUri
    "http://request.example.com:8083/user/12345" | "host.example.com:8082" || "https://request.example.com:8083"
    "/user/12345"                                | "host.example.com:8082" || "https://host.example.com:8082"
    "/user/12345"                                | null                    || "https://bind.example.com:8081"
  }

  def "X-Forwarded-Ssl headers are supported"() {
    given:
    def bindAddress = mockBindAddress(8081, "bind.example.com")
    def publicAddress = new DefaultPublicAddress(null, HTTP_SCHEME)

    when: "The request includes a X-Forwarded-Proto header and no public URL is defined"
    def context = mockContext(mockRequest(uri, mockHeaders([(HOST):host, (X_FORWARDED_SSL): ON.toString()])), bindAddress)
    def address = publicAddress.getAddress(context)

    then: "The protocol is based on the header value in preference to absolute request URI or service scheme"
    address.toString() == expectedUri

    where:
    uri                                          | host                    || expectedUri
    "http://request.example.com:8083/user/12345" | "host.example.com:8082" || "https://request.example.com:8083"
    "/user/12345"                                | "host.example.com:8082" || "https://host.example.com:8082"
    "/user/12345"                                | null                    || "https://bind.example.com:8081"
  }

  private static Headers mockHeaders(Map<CharSequence, String> entries) {
    def headers = new DefaultHttpHeaders()
    entries.each {
      if (it.value) {
        headers.add(it.key, it.value)
      }
    }
    return new NettyHeadersBackedMutableHeaders(headers)
  }

  private Headers mockHostHeaders(String host) {
    def headers = Mock(Headers)
    headers.get(HOST.toString()) >> host
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
