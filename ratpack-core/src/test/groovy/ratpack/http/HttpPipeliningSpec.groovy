/*
 * Copyright 2023 the original author or authors.
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

package ratpack.http


import io.netty.buffer.Unpooled
import ratpack.exec.Promise
import ratpack.stream.Streams
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.util.concurrent.BlockingVariable

import java.time.Duration

// Note: Ratpack does not process pipelined requests concurrently
class HttpPipeliningSpec extends RatpackGroovyDslSpec {

  def "attempt to pipeline requests causes server to close connection"() {
    when:

    handlers {
      post("delay") {
        render(request.body.map { it.text }.defer(Duration.ofMillis(10)))
      }
      post("no-delay") {
        render(request.body.map { it.text })
      }
    }

    and:
    def socket = withSocket {
      3.times {
        write("POST /delay HTTP/1.1\r\n")
        write("content-length: 2\r\n")
        write("\r\n")
        write("${it}a")
        flush()
        write("POST /no-delay HTTP/1.1\r\n")
        write("content-length: 2\r\n")
        write("\r\n")
        write("${it}b")
        flush()
      }
      write("POST /no-delay HTTP/1.1\r\n")
      write("content-length: 3\r\n")
      write("connection: close\r\n")
      write("\r\n")
      write("end")
      flush()
    }

    then:
    def responses = socket.inputStream.text.normalize()
    responses == """HTTP/1.1 200 OK
content-type: text/plain;charset=UTF-8
content-length: 2

0aHTTP/1.1 200 OK
content-type: text/plain;charset=UTF-8
content-length: 2

0bHTTP/1.1 200 OK
content-type: text/plain;charset=UTF-8
content-length: 2

1aHTTP/1.1 200 OK
content-type: text/plain;charset=UTF-8
content-length: 2

1bHTTP/1.1 200 OK
content-type: text/plain;charset=UTF-8
content-length: 2

2aHTTP/1.1 200 OK
content-type: text/plain;charset=UTF-8
content-length: 2

2bHTTP/1.1 200 OK
content-type: text/plain;charset=UTF-8
content-length: 3
connection: close

end"""
  }

  def "can handle next request being sent while transmitting previous response"() {
    when:
    def releaser = new BlockingVariable<Runnable>()

    handlers {
      get("first") {
        response.sendStream(Streams.publish(Promise.value([Unpooled.wrappedBuffer("abc".bytes)]).defer(releaser.&set)))
      }
      get("second") {
        render("ok")
      }
    }

    and:
    def socket = withSocket {
      write("GET /first HTTP/1.1\r\n")
      write("\r\n")
      write("GET /second HTTP/1.1\r\n")
      write("Connection: close\r\n")
      write("\r\n")
      flush()
    }
    releaser.get().run()

    then:
    def responses = socket.inputStream.text.normalize()
    responses == """HTTP/1.1 200 OK
transfer-encoding: chunked

3
abc
0

HTTP/1.1 200 OK
content-type: text/plain;charset=UTF-8
content-length: 2
connection: close

ok"""
  }
}
