/*
 * Copyright 2015 the original author or authors.
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

import org.slf4j.Logger
import org.slf4j.Marker
import org.slf4j.helpers.MessageFormatter
import ratpack.test.internal.RatpackGroovyDslSpec

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class RequestLogSpec extends RatpackGroovyDslSpec {

  def "add request logging"() {
    def messages = []
    def latch = new CountDownLatch(2)
    def logger = Mock(Logger) {
      _ * isInfoEnabled(_ as Marker) >> true
      2 * info(_ as Marker, _ as String, _ as Object[] ) >> { Marker marker, String msgPattern, Object[] params -> messages << MessageFormatter.arrayFormat(msgPattern, params).message; latch.countDown() }
    }

    given:
    handlers {
      all RequestLogger.ncsa(logger)
      path("foo") {
        render "ok"
      }
      path("bar") {
        render "ok"
      }
    }

    when:
    get("foo")
    post("bar")

    then:
    latch.await(10, TimeUnit.SECONDS)
  }
}
