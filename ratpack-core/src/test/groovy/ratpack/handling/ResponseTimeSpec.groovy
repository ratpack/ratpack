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

import ratpack.exec.Blocking
import ratpack.test.internal.RatpackGroovyDslSpec

class ResponseTimeSpec extends RatpackGroovyDslSpec {

  public static final String DECIMAL_NUMBER = ~/\d+\.\d+/

  def "does not contain response time header by default"() {
    when:
    handlers {
      all { response.send() }
    }

    then:
    with(get()) {
      headers.get("X-Response-Time") == null
    }
  }

  def "does contain response time header if enabled"() {
    given:
    bindings {
      bindInstance ResponseTimer.decorator()
    }

    when:
    handlers {
      all { response.send() }
    }

    then:
    with(get()) {
      headers.get("X-Response-Time") ==~ DECIMAL_NUMBER
    }
  }

  def "does contain response time header when blocking operation used"() {
    given:
    bindings {
      bindInstance ResponseTimer.decorator()
    }

    when:
    handlers {
      all {
        Blocking.get { sleep 100 } then { response.send() }
      }
    }

    then:
    with(get()) {
      headers.get("X-Response-Time") ==~ DECIMAL_NUMBER
    }
  }

  def "static files have no response time when not enabled"() {
    given:
    write("files/foo.txt", "foo")

    when:
    handlers {
      files { dir "files" }
    }

    then:
    with(get("foo.txt")) {
      body.text == "foo"
      headers.get("X-Response-Time") == null
    }
  }

  def "static files have response time when enabled"() {
    given:
    bindings {
      bindInstance ResponseTimer.decorator()
    }

    and:
    write("files/foo.txt", "foo")

    when:
    handlers {
      files { dir "files" }
    }

    then:
    with(get("foo.txt")) {
      body.text == "foo"
      headers.get("X-Response-Time") ==~ DECIMAL_NUMBER
    }
  }

}
