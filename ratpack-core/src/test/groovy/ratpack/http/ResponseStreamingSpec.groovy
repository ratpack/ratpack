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

package ratpack.http

import ratpack.test.internal.RatpackGroovyDslSpec

import static ratpack.http.ResponseChunks.stringChunks
import static ratpack.stream.Streams.publish

class ResponseStreamingSpec extends RatpackGroovyDslSpec {

  def "too large unread request body is silently discarded when streaming a response"() {
    when:
    handlers {
      all {
        request.maxContentLength = 12
        render stringChunks(
          publish(["abc"] * 3)
        )
      }
    }

    then:
    def r = request { it.post().body.text("a" * 100_000) }
    r.status.code == 200
    r.body.text == "abc" * 3
  }

  def "unread request body is silently discarded when streaming a response"() {
    when:
    handlers {
      all {
        request.maxContentLength = 100_000
        render stringChunks(
          publish(["abc"] * 3)
        )
      }
    }

    then:
    def r = request { it.post().body.text("a" * 1000) }
    r.status.code == 200
    r.body.text == "abc" * 3
  }
}
