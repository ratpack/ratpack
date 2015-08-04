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

import groovy.io.GroovyPrintStream
import org.slf4j.LoggerFactory
import ratpack.test.internal.RatpackGroovyDslSpec

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


class RequestLogSpec extends RatpackGroovyDslSpec {

  def "add request logging"() {
    def loggerOutput = new ByteArrayOutputStream()
    def logger = LoggerFactory.getLogger(RequestLogger)
    def originalStream = logger.TARGET_STREAM
    def latch = new CountDownLatch(2)
    logger.TARGET_STREAM = new GroovyPrintStream(loggerOutput) {

      @Override
      void println(String s) {
        super.println(s)
        originalStream.println(s)
        if (s.contains(RequestLogger.simpleName)) {
          latch.countDown()
        }
      }
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
    latch.await(5, TimeUnit.SECONDS)
    String output = loggerOutput.toString()
    output.contains("\"GET /foo HTTP/1.1\" 200 2")
    output.contains("\"POST /bar HTTP/1.1\" 200 2")
  }
}
