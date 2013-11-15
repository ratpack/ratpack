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

package ratpack.test.handling

import io.netty.util.CharsetUtil
import ratpack.handling.Context
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import static ratpack.groovy.Groovy.groovyHandler
import static ratpack.test.UnitTest.invocationBuilder

class InvocationBuilderSpec extends Specification {

  @Subject
  InvocationBuilder builder = invocationBuilder()

  @Delegate
  Invocation invocation

  void invoke(@DelegatesTo(Context) Closure handler) {
    invocation = builder.invoke(groovyHandler(handler))
  }

  def "can test handler that just calls next"() {
    when:
    invoke { next() }

    then:
    bodyText == null
    bodyBytes == null
    calledNext
    !sentResponse
    exception == null
    sentFile == null
  }

  def "can test handler that sends string"() {
    when:
    invoke { response.send "foo" }

    then:
    bodyText == "foo"
    bodyBytes == "foo".getBytes(CharsetUtil.UTF_8)
    !calledNext
    sentResponse
    exception == null
    sentFile == null
    headers.get("content-type") == "text/plain;charset=UTF-8"
  }

  def "can test handler that sends bytes"() {
    when:
    invoke { response.send "foo".getBytes(CharsetUtil.UTF_8) }

    then:
    bodyText == "foo"
    bodyBytes == "foo".getBytes(CharsetUtil.UTF_8)
    !calledNext
    sentResponse
    exception == null
    headers.get("content-type") == "application/octet-stream"
    sentFile == null
  }

  def "can test handler that sends file"() {
    when:
    invoke { response.sendFile background, "text/plain", new File("foo") }

    then:
    bodyText == null
    bodyBytes == null
    !calledNext
    !sentResponse
    exception == null
    sentFile == new File("foo")
    headers.get("content-type") == "text/plain;charset=UTF-8"
  }

  def "can register things"() {
    given:
    builder.register "foo"

    when:
    invoke { response.send get(String) }

    then:
    bodyText == "foo"
  }

  def "can test async handlers"() {
    given:
    builder.timeout 3

    when:
    invoke { Thread.start { sleep 1000; next() } }

    then:
    calledNext
  }

  def "will throw if handler takes too long"() {
    given:
    builder.timeout 1

    when:
    invoke { Thread.start { sleep 2000; next() } }

    then:
    thrown InvocationTimeoutException
  }

  def "can set uri"() {
    given:
    builder.uri "foo"

    when:
    invoke { response.send request.uri }

    then:
    bodyText == "/foo"
  }

  def "can set request method"() {
    given:
    builder.method "PUT"

    when:
    invoke { response.send request.method.name }

    then:
    bodyText == "PUT"
  }

  def "can set request headers"() {
    given:
    builder.header "X-Requested-With", "Spock"

    when:
    invoke { response.send request.headers.get("X-Requested-With") }

    then:
    bodyText == "Spock"
  }

  def "can set response headers"() {
    given:
    builder.responseHeader "Via", "Ratpack"

    when:
    invoke { response.send response.headers.get("Via") }

    then:
    bodyText == "Ratpack"
  }

  @Unroll
  def "can set request body"() {
    //noinspection GroovyAssignabilityCheck
    given:
    builder.body(* arguments)

    when:
    invoke {
      response.headers.set "X-Request-Content-Length", request.headers.get("Content-Length")
      response.headers.set "X-Request-Content-Type", request.headers.get("Content-Type")
      response.send request.bytes
    }

    then:
    bodyBytes == responseBytes
    headers.get("X-Request-Content-Type") == responseContentType
    headers.get("X-Request-Content-Length") == "$responseBytes.length"

    where:
    arguments                             | responseContentType        | responseBytes
    [[0, 1, 2, 4] as byte[], "image/png"] | "image/png"                | [0, 1, 2, 4] as byte[]
    ["foo", "text/plain"]                 | "text/plain;charset=UTF-8" | "foo".bytes
  }

  @Unroll
  def "can set response body"() {
    //noinspection GroovyAssignabilityCheck
    given:
    builder.responseBody(* arguments)

    when:
    invoke {
      // just pass through the existing response
      response.send()
    }

    then:
    bodyBytes == responseBytes
    headers.get("Content-Type") == responseContentType

    where:
    arguments                             | responseContentType        | responseBytes
    [[0, 1, 2, 4] as byte[], "image/png"] | "image/png"                | [0, 1, 2, 4] as byte[]
    ["foo", "text/plain"]                 | "text/plain;charset=UTF-8" | "foo".bytes
  }
}
