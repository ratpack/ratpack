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

package ratpack.handling.internal

import org.slf4j.Logger
import ratpack.handling.RequestLogger
import ratpack.test.internal.RatpackGroovyDslSpec

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

class NcsaRequestLoggerSpec extends RatpackGroovyDslSpec {

  def "request log includes query string"() {
    given:
    def msgQueue = new ArrayBlockingQueue<String>(1)
    def logger = Mock(Logger) {
      isInfoEnabled() >> true
      info(_ as String) >> { String msg -> msgQueue << msg }
    }

    handlers {
      all RequestLogger.ncsa(logger)
      path("foo") {
        render "hello"
      }
    }

    when:
    getText("foo?bar=baz")

    then:
    msgQueue.poll(2, TimeUnit.SECONDS).contains("\"GET /foo?bar=baz HTTP/1.1\"")
  }
}
