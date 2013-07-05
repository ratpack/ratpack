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

package org.ratpackframework.http.internal

import io.netty.channel.Channel
import io.netty.handler.codec.http.DefaultHttpHeaders
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpVersion
import spock.lang.Specification
import spock.lang.Unroll

class DefaultResponseSpec extends Specification {

  DefaultResponse response
  def request

  def setup() {
    def nettyResponse = Mock(FullHttpResponse)
    def channel = Mock(Channel)
    request = Mock(FullHttpRequest)

    response = new DefaultResponse(nettyResponse, channel, true, HttpVersion.HTTP_1_1, request)
  }


  def "generateRedirectLocation Absolute URL"() {
    given:
    def absURL = "http://www.google.com"

    expect:
    response.generateRedirectLocation(absURL) == absURL
  }

  def "generateRedirectLocation Starting Slash URL no Server Default no host"() {
    given:
    def path = "/index"
    def headers = new DefaultHttpHeaders()

    when:
    1 * request.headers() >> headers

    then:
    response.generateRedirectLocation(path) == path
  }

  def "generateRedirectLocation Starting Slash URL no Server Default"() {
    given:
    def path = "/index"
    def headers = new DefaultHttpHeaders()
    headers.add("Host", "example.com")

    when:
    2 * request.headers() >> headers

    then:
    response.generateRedirectLocation(path) == "http://example.com" + path
  }

  @Unroll
  def "getParentPath #path - #parent"() {
    expect:
    response.getParentPath(path) == parent

    where:
    path             | parent
    "/a/b"           | "/a/"
    "nothing"        | "/"
    "cool/hand/luke" | "/cool/hand/"
  }

}
