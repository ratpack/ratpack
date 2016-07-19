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

package ratpack.http.stream

import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil
import ratpack.test.internal.RatpackGroovyDslSpec

import java.nio.charset.Charset

import static ratpack.http.ResponseChunks.bufferChunks
import static ratpack.http.ResponseChunks.stringChunks
import static ratpack.stream.Streams.publish

class ChunkedResponseSpec extends RatpackGroovyDslSpec {

  def "can send chunked strings"() {
    given:
    handlers {
      all {
        render stringChunks(
          publish(["abcü"] * 3)
        )
      }
    }

    expect:
    rawResponse() == """HTTP/1.1 200 OK
transfer-encoding: chunked
content-type: text/plain;charset=UTF-8
connection: close

5
abcü
5
abcü
5
abcü
0

"""
  }

  def "can send chunked bytes"() {
    given:
    handlers {
      all {
        render bufferChunks(
          "text/plain;charset=UTF-8",
          publish((1..3).collect { Unpooled.copiedBuffer("abcü", CharsetUtil.UTF_8) })
        )
      }
    }

    expect:
    rawResponse() == """HTTP/1.1 200 OK
transfer-encoding: chunked
content-type: text/plain;charset=UTF-8
connection: close

5
abcü
5
abcü
5
abcü
0

"""
  }


  String rawResponse(Charset charset = CharsetUtil.UTF_8) {
    StringBuilder builder = new StringBuilder()
    Socket socket = new Socket(getAddress().host, getAddress().port)
    try {
      new OutputStreamWriter(socket.outputStream, "UTF-8").with {
        write("GET / HTTP/1.1\r\n")
        write("Connection: close\r\n")
        write("\r\n")
        flush()
      }

      InputStreamReader inputStreamReader = new InputStreamReader(socket.inputStream, charset)
      BufferedReader bufferedReader = new BufferedReader(inputStreamReader)

      def chunk
      while ((chunk = bufferedReader.readLine()) != null) {
        builder.append(chunk).append("\n")
      }

      builder.toString()
    } finally {
      socket.close()
    }
  }

}
