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

import org.ratpackframework.handling.Context
import org.ratpackframework.handling.Handler
import spock.lang.Specification

import static org.ratpackframework.groovy.test.handling.InvocationBuilder.invoke

/**
 * This is not so much testing our stuff, but acting as an example of how to use the invocation builder.
 */
class InvocationBuilderExampleSpec extends Specification {

  static class MyHandler implements Handler {
    void handle(Context context) {
      context.with {
        response.headers.set("set-header", "set")
        response.send request.headers.get("test-header") + ":" + request.uri
      }
    }
  }

  def "can unit test handler"() {
    when:
    def invocation = invoke(new MyHandler()) {
      requestHeaders.set("Test-Header", "foo")
      uri = "/bar"
    }

    then:
    with(invocation) {
      bodyText == "foo:/bar"
      headers.get("set-header") == "set"
    }
  }

}
