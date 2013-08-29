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

package org.ratpackframework.test.handling

import io.netty.util.CharsetUtil
import org.ratpackframework.handling.Context
import spock.lang.Specification
import spock.lang.Subject

import static org.ratpackframework.groovy.Util.asHandler

class InvocationBuilderSpec extends Specification {

  @Subject InvocationBuilder builder = new InvocationBuilder()
  @Delegate Invocation invocation

  void invoke(@DelegatesTo(Context) Closure handler) {
    invocation = builder.invoke(asHandler(handler))
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
    invoke { response.sendFile blocking, "text/plain", new File("foo") }

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
    builder.timeout = 3

    when:
    invoke { Thread.start { sleep 1000; next() } }

    then:
    calledNext
  }

  def "will throw if handler takes too long"() {
    given:
    builder.timeout = 1

    when:
    invoke { Thread.start { sleep 2000; next() } }

    then:
    thrown InvocationTimeoutException
  }

  def "can set uri"() {
    given:
    builder.uri = "foo"

    when:
    invoke { response.send request.uri }

    then:
    bodyText == "/foo"
  }
}
