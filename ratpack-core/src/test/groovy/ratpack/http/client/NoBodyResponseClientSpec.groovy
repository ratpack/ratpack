/*
 * Copyright 2017 the original author or authors.
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

package ratpack.http.client

import io.netty.handler.codec.http.HttpResponseStatus
import ratpack.stream.bytebuf.ByteBufStreams

class NoBodyResponseClientSpec extends BaseHttpClientSpec {

  def "no content length is sent for #status response"() {
    when:
    otherApp {
      get {
        response.status(status.code()).send()
      }
    }
    handlers {
      get {
        get(HttpClient).get(otherAppUrl()).then {
          assert !it.headers.contains("content-length")
          assert it.body.buffer.readableBytes() == 0

          render "ok"
        }
      }
    }

    then:
    text == "ok"

    where:
    status << noBodyResponseStatuses()

  }

  def "#status response is handled when server sends a body"() {
    when:
    otherApp {
      get {
        response.status(status.code()).send("foo")
      }
    }
    handlers {
      get {
        get(HttpClient).get(otherAppUrl()).then {
          // Note: this behaviour is really down to Netty.
          // We can't practically make it let the body through.

          if (status.code() == 304) {
            assert it.headers.get("content-length") == "3"
          } else {
            assert !it.headers.contains("content-length")
          }
          assert it.body.buffer.readableBytes() == 0

          render "ok"
        }
      }
    }

    then:
    text == "ok"

    where:
    status << noBodyResponseStatuses()

  }

  def "no content length is sent for streaming #status response"() {
    when:
    otherApp {
      get {
        response.status(status.code()).send()
      }
    }
    handlers {
      get {
        get(HttpClient).requestStream(otherAppUrl(), {}).then {
          assert !it.headers.contains("content-length")

          ByteBufStreams.toByteArray(it.body).then {
            assert it.length == 0
            render "ok"
          }
        }
      }
    }

    then:
    text == "ok"

    where:
    status << noBodyResponseStatuses()

  }

  def "#status streaming response is handled when server sends a body"() {
    when:
    otherApp {
      get {
        response.status(status.code()).send("foo")
      }
    }
    handlers {
      get {
        get(HttpClient).requestStream(otherAppUrl(), {}).then {
          if (status.code() == 304) {
            assert it.headers.get("content-length") == "3"
          } else {
            assert !it.headers.contains("content-length")
          }

          ByteBufStreams.toByteArray(it.body).then {
            assert it.length == 0
            render "ok"
          }
        }
      }
    }

    then:
    text == "ok"

    where:
    status << noBodyResponseStatuses()

  }

  static List<HttpResponseStatus> noBodyResponseStatuses() {
    [HttpResponseStatus.valueOf(100), HttpResponseStatus.valueOf(150), HttpResponseStatus.valueOf(199), HttpResponseStatus.valueOf(204), HttpResponseStatus.valueOf(304)]
  }

}
