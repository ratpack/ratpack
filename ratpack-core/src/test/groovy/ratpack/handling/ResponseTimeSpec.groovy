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

package ratpack.handling

import ratpack.test.internal.RatpackGroovyDslSpec

class ResponseTimeSpec extends RatpackGroovyDslSpec {

  public static final String DECIMAL_NUMBER = ~/\d+\.\d+/

  def "does not contain response time header by default"() {
    when:
    handlers {
      handler { response.send() }
    }

    then:
    with(get()) {
      headers.get("X-Response-Time") == null
    }
  }

  def "does contain response time header if enabled"() {
    given:
    launchConfig {
      timeResponses true
    }

    when:
    handlers {
      handler { response.send() }
    }

    then:
    with(get()) {
      headers.get("X-Response-Time").value ==~ DECIMAL_NUMBER
    }
  }

  def "does contain response time header when background used"() {
    given:
    launchConfig {
      timeResponses true
    }

    when:
    handlers {
      handler {
        background { sleep 100 } then { response.send() }
      }
    }

    then:
    with(get()) {
      headers.get("X-Response-Time").value ==~ DECIMAL_NUMBER
    }
  }

  def "static files have no response time when not enabled"() {
    given:
    file("files/foo.txt", "foo")

    when:
    handlers {
      assets "files"
    }

    then:
    with(get("foo.txt")) {
      body.asString() == "foo"
      headers.get("X-Response-Time") == null
    }
  }

  def "static files have response time when enabled"() {
    given:
    launchConfig {
      timeResponses true
    }

    and:
    file("files/foo.txt", "foo")

    when:
    handlers {
      assets "files"
    }

    then:
    with(get("foo.txt")) {
      body.asString() == "foo"
      headers.get("X-Response-Time").value ==~ DECIMAL_NUMBER
    }
  }

}
