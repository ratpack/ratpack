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

import com.google.common.net.HostAndPort
import io.netty.handler.codec.http.DefaultHttpHeaders
import ratpack.exec.Promise
import ratpack.http.Headers
import ratpack.http.Request
import ratpack.http.internal.NettyHeadersBackedMutableHeaders
import ratpack.server.PublicAddress
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification

import static ratpack.http.internal.HttpHeaderConstants.*
import static ratpack.util.internal.ProtocolUtil.HTTPS_SCHEME
import static ratpack.util.internal.ProtocolUtil.HTTP_SCHEME

class PublicAddressSpec extends Specification {

  @AutoCleanup
  def harness = ExecHarness.harness()

  String get(PublicAddress publicAddress, Request request) {
    harness.yield({ it.add(Request, request) }, {
      Promise.value(publicAddress.get().toString())
    }).value
  }

  def "Get URL #publicURL, #scheme, #requestUri, #headers, #bindHost:#bindPort -> #expected"() {
    given:
    def publicAddress = get(publicURL ? PublicAddress.of(new URI(publicURL)) : PublicAddress.inferred(scheme), mockRequest(requestUri, mockHeaders(headers), HostAndPort.fromParts(bindHost, bindPort)))

    expect:
    publicAddress == expected

    where:
    publicURL                       | scheme       | requestUri                                    | headers                           | bindHost            | bindPort || expected

    "http://conf.example.com"       | HTTP_SCHEME  | "http://request.example.com/user/12345"       | [(HOST): "host.example.com"]      | "localhost"         | 80       || "http://conf.example.com"
    "https://conf.example.com"      | HTTP_SCHEME  | "http://request.example.com/user/12345"       | [(HOST): "host.example.com"]      | "localhost"         | 8080     || "https://conf.example.com"
    "https://conf.example.com:8443" | HTTPS_SCHEME | "http://request.example.com/user/12345"       | [(HOST): "host.example.com"]      | "localhost"         | 8443     || "https://conf.example.com:8443"

    null                            | HTTP_SCHEME  | "http://request.example.com/user/12345"       | [(HOST): "host.example.com"]      | "localhost"         | 80       || "http://request.example.com"
    null                            | HTTPS_SCHEME | "https://request.example.com:8443/user/12345" | [(HOST): "host.example.com"]      | "localhost"         | 8443     || "https://request.example.com:8443"

    null                            | HTTP_SCHEME  | "http://request.example.com/user/12345"       | [(X_FORWARDED_HOST) : "fhost.example.com",
                                                                                                      (HOST)             : "host.example.com",
                                                                                                      (X_FORWARDED_PROTO): "https"]    | "localhost"         | 8080     || "https://fhost.example.com"
    null                            | HTTP_SCHEME  | "/user/12345"                                 | [(HOST)             : "host.example.com",
                                                                                                      (X_FORWARDED_PROTO): "https"]    | "localhost"         | 8080     || "https://host.example.com"
    null                            | HTTP_SCHEME  | "/user/12345"                                 | [(HOST)             : "host.example.com",
                                                                                                      (X_FORWARDED_PROTO): "https"]    | "localhost"         | 8080     || "https://host.example.com"
    null                            | HTTP_SCHEME  | "/user/12345"                                 | [(HOST)           : "host.example.com",
                                                                                                      (X_FORWARDED_SSL): "on"]         | "localhost"         | 8080     || "https://host.example.com"

    null                            | HTTP_SCHEME  | "/user/12345"                                 | [(HOST): "host.example.com"]      | "localhost"         | 8080     || "http://host.example.com"
    null                            | HTTPS_SCHEME | "/user/12345"                                 | [(HOST): "host.example.com"]      | "localhost"         | 8443     || "https://host.example.com"
    null                            | HTTP_SCHEME  | "/user/12345"                                 | [(HOST): "host.example.com:8080"] | "localhost"         | 8080     || "http://host.example.com:8080"
    null                            | HTTPS_SCHEME | "/user/12345"                                 | [(HOST): "host.example.com:8443"] | "localhost"         | 8443     || "https://host.example.com:8443"

    null                            | HTTP_SCHEME  | "/user/12345"                                 | [:]                               | "localhost"         | 80       || "http://localhost"
    null                            | HTTP_SCHEME  | "/user/12345"                                 | [:]                               | "localhost"         | 8080     || "http://localhost:8080"
    null                            | HTTPS_SCHEME | "/user/12345"                                 | [:]                               | "localhost"         | 443      || "https://localhost"
    null                            | HTTPS_SCHEME | "/user/12345"                                 | [:]                               | "localhost"         | 8443     || "https://localhost:8443"
    null                            | HTTP_SCHEME  | "/user/12345"                                 | [:]                               | "[0:0:0:0:0:0:0:1]" | 5050     || "http://[0:0:0:0:0:0:0:1]:5050"
  }

  def "Absolute request URIs are supported: #uri -> #expectedUri"() {
    given:
    def bindAddress = HostAndPort.fromParts("bind.example.com", 8080)
    def publicAddress = new InferringPublicAddress(new URI(uri).scheme)

    when: "The request URI is absolute"
    def address = get(publicAddress, mockRequest(uri, mockHeaders([(HOST): "host.example.com"]), bindAddress))

    then: "The host is part of the request URI and any Host header MUST be ignored"
    and: "The protocol and port are based on the request URI"
    address.toString() == expectedUri

    where:
    uri                                           || expectedUri
    "http://request.example.com/user/12345"       || "http://request.example.com"
    "https://request.example.com:8443/user/12345" || "https://request.example.com:8443"
  }

  def "Host headers are supported: #scheme, #host -> #expectedUri"() {
    given:
    def bindAddress = HostAndPort.fromParts("bind.example.com", 8080)
    def publicAddress = new InferringPublicAddress(scheme)

    when: "The request URI is not absolute and the request includes a Host header"
    def address = get(publicAddress, mockRequest("/user/12345", mockHeaders([(HOST): host]), bindAddress))

    then: "The host is determined by the Host header value"
    and: "The port is based on the Host header value (absent means default for scheme)"
    address.toString() == expectedUri

    where:
    scheme       | host                    || expectedUri
    HTTP_SCHEME  | "host.example.com"      || "http://host.example.com"
    HTTPS_SCHEME | "host.example.com"      || "https://host.example.com"
    HTTP_SCHEME  | "host.example.com:8080" || "http://host.example.com:8080"
    HTTPS_SCHEME | "host.example.com:8080" || "https://host.example.com:8080"
  }

  def "X-Forwarded-Host header is supported: #uri, #fhost, #host -> #expectedUri"() {
    given:
    def bindAddress = HostAndPort.fromParts("bind.example.com", 8080)
    def publicAddress = new InferringPublicAddress(HTTP_SCHEME)

    when: "The request includes a X-Forwarded-Host header and no public URL is defined"
    def address = get(publicAddress, mockRequest("/user/12345", mockHeaders([(X_FORWARDED_HOST): fhost, (HOST): host]), bindAddress))

    then: "The forwarded host is used in place of the absolute request URI and Host header"
    address.toString() == expectedUri

    where:
    uri                               | fhost                    | host                    || expectedUri
    "http://request.example.com"      | "fhost.example.com"      | null                    || "http://fhost.example.com"
    "http://request.example.com:8081" | "fhost.example.com:8082" | "host.example.com:8083" || "http://fhost.example.com:8082"
  }

  def "X-Forwarded-Host header supports proxy chains"() {
    given:
    def bindAddress = HostAndPort.fromParts("bind.example.com", 8080)
    def publicAddress = new InferringPublicAddress(HTTP_SCHEME)

    when: "The request includes a X-Forwarded-Host header with multiple comma-separated entries"
    def address = get(publicAddress, mockRequest("/user/12345", mockHeaders([
      (X_FORWARDED_HOST): "fhost1.example.com:8081, fhost2.example.com:8082",
      (HOST)            : "host.example.com:8083"
    ]), bindAddress))

    then: "The first entry is used"
    address.toString() == "http://fhost1.example.com:8081"
  }

  def "X-Forwarded-Proto headers are supported: #uri, #host -> #expectedUri"() {
    given:
    def bindAddress = HostAndPort.fromParts("bind.example.com", 8081)
    def publicAddress = new InferringPublicAddress(HTTP_SCHEME)

    when: "The request includes a X-Forwarded-Proto header and no public URL is defined"
    def address = get(publicAddress, mockRequest(uri, mockHeaders([(HOST): host, (X_FORWARDED_PROTO): HTTPS_SCHEME]), bindAddress))

    then: "The protocol is based on the header value in preference to absolute request URI or service scheme"
    address.toString() == expectedUri

    where:
    uri                                          | host                    || expectedUri
    "http://request.example.com:8083/user/12345" | "host.example.com:8082" || "https://request.example.com:8083"
    "/user/12345"                                | "host.example.com:8082" || "https://host.example.com:8082"
    "/user/12345"                                | null                    || "https://bind.example.com:8081"
  }

  def "X-Forwarded-Ssl headers are supported: #uri, #host -> #expectedUri"() {
    given:
    def bindAddress = HostAndPort.fromParts("bind.example.com", 8081)
    def publicAddress = new InferringPublicAddress(HTTP_SCHEME)

    when: "The request includes a X-Forwarded-Proto header and no public URL is defined"
    def address = get(publicAddress, mockRequest(uri, mockHeaders([(HOST): host, (X_FORWARDED_SSL): ON.toString()]), bindAddress))

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

  private Request mockRequest(String rawUri, Headers headers, HostAndPort localAddress) {
    return Mock(Request) {
      getRawUri() >> rawUri
      getHeaders() >> headers
      getLocalAddress() >> localAddress
    }
  }

}
