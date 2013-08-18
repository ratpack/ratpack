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
import org.ratpackframework.groovy.handling.ClosureHandlers
import org.ratpackframework.handling.Context
import spock.lang.Specification

import static org.ratpackframework.groovy.Util.delegatingAction

class InvocationBuilderSpec extends Specification {

  @Delegate Invocation invocation

  void invoke(@DelegatesTo(Context) Closure handler, @DelegatesTo(InvocationBuilder) Closure build) {
    invocation = InvocationBuilder.invoke(ClosureHandlers.handler(handler), delegatingAction(build))
  }

  def "can test handler that just calls next"() {
    when:
    invoke({ next() }, {},)

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
    invoke({ response.send "foo" }, {},)

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
    invoke({ response.send "foo".getBytes(CharsetUtil.UTF_8) }, {},)

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
    invoke({ response.sendFile blocking, "text/plain", new File("foo") }, {},)

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
    when:
    invoke({ response.send get(String) }, { register new String("foo") })

    then:
    bodyText == "foo"
  }

  def "can test async handlers"() {
    when:
    invoke({ Thread.start { sleep 1000; next() } }, { timeout = 3 })

    then:
    calledNext
  }

  def "will throw if handler takes too long"() {
    when:
    invoke({ Thread.start { sleep 2000; next() } }, { timeout = 1 })

    then:
    thrown InvocationTimeoutException
  }

  def "can set uri"() {
    when:
    invoke({ response.send request.uri }, { uri = "foo" })

    then:
    bodyText == "/foo"
  }
}
