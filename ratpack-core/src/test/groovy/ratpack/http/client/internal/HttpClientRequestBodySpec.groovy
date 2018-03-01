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

package ratpack.http.client.internal

import io.netty.buffer.UnpooledByteBufAllocator
import ratpack.http.client.BaseHttpClientSpec
import ratpack.http.client.HttpClient

class HttpClientRequestBodySpec extends BaseHttpClientSpec {

  def setup() {
    otherApp {
      all {
        render request.body.map { it.text }
      }
    }
  }

  def "using text as request body"() {
    when:
    handlers {
      get { HttpClient http ->
        render http.post(otherAppUrl()) {
          it.body.text("foobar")
        }.map { it.body.text }
      }
    }

    then:
    text == "foobar"

    where:
    tmp << (1..30)
  }

  def "using incoming request body as outgoing request body"() {
    when:
    handlers {
      post { HttpClient http ->
        request.body.then { b ->
          render http.post(otherAppUrl()) {
            it.body.buffer(b.buffer)
          }.map { it.body.text }
        }
      }
    }

    then:
    request { it.post().body.text("foobar") }.body.text == "foobar"

    where:
    tmp << (1..30)
  }

  def "using buffer as request body"() {
    when:
    handlers {
      get { HttpClient http ->
        render http.post(otherAppUrl()) {
          it.body.buffer(UnpooledByteBufAllocator.DEFAULT.buffer().writeBytes("foobar".bytes))
        }.map { it.body.text }
      }
    }

    then:
    text == "foobar"

    where:
    tmp << (1..30)
  }

  def "using buffer as request body where config fails"() {
    when:
    handlers {
      get { HttpClient http ->
        render http.post(otherAppUrl()) {
          it.body.buffer(UnpooledByteBufAllocator.DEFAULT.buffer().writeBytes("foobar".bytes))
          throw new Exception("!")
        }.map { it.body.text }
      }
    }

    then:
    get().statusCode == 500

    where:
    tmp << (1..30)
  }


}
