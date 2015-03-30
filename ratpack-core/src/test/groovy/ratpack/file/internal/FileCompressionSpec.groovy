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

package ratpack.file.internal

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import ratpack.test.internal.RatpackGroovyDslSpec

import java.nio.charset.StandardCharsets

class FileCompressionSpec extends RatpackGroovyDslSpec {

  def content = "abc" * 1000
  def bytes = content.getBytes(StandardCharsets.US_ASCII)

  def setup() {
    file("public/file.txt") << bytes
  }

  void requestCompression(boolean flag) {
    requestSpec {
      it.headers {
        it.set(HttpHeaderNames.ACCEPT_ENCODING, flag ? "gzip" : HttpHeaderValues.IDENTITY)
      }
    }
  }

  def "doesn't encode when compression disabled"() {
    given:
    requestCompression(true)
    handlers {
      handler { it.response.noCompress(); it.next() }
      assets "public"
    }

    when:
    def response = get("file.txt")

    then:
    response.headers.get("Content-Encoding") == null
    response.headers.get("Content-Length") == bytes.length.toString()
  }

  def "encodes when requested"() {
    when:
    requestCompression(true)
    handlers {
      assets "public"
    }

    then:
    get("file.txt")
    response.headers.get("Content-Encoding") == "gzip"
    response.headers.get("Content-Length").toInteger() < bytes.length
  }

}
