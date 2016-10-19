/*
 * Copyright 2016 the original author or authors.
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

import io.netty.handler.codec.http.HttpHeaderNames
import ratpack.test.internal.RatpackGroovyDslSpec

import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

class IfSinceModifiedSpec extends RatpackGroovyDslSpec {

  def "does not send if resource has not been modified since"() {
    when:
    def instant = Instant.now()
    def i = new AtomicInteger()
    handlers {
      get {
        lastModified(instant) {
          render Integer.toString(i.getAndIncrement())
        }
      }
    }

    then:
    text == "0"
    text == "1"
    request { it.headers.setDate(HttpHeaderNames.IF_MODIFIED_SINCE, instant) }.statusCode == 304
    request { it.headers.setDate(HttpHeaderNames.IF_MODIFIED_SINCE, instant.plusSeconds(1)) }.statusCode == 304
    request { it.headers.setDate(HttpHeaderNames.IF_MODIFIED_SINCE, instant.minusSeconds(1)) }.body.text == "2"
  }
}
