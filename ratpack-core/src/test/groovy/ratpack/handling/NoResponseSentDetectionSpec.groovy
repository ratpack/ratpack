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

package ratpack.handling

import ratpack.exec.Promise
import ratpack.http.Status
import ratpack.test.internal.RatpackGroovyDslSpec

class NoResponseSentDetectionSpec extends RatpackGroovyDslSpec {

  def setup() {
    serverConfig {
      development(true)
    }
  }

  def "sends empty 500 when no response is sent"() {
    when:
    handlers {
      get {
        // no response sent here, no next() called
      }
    }

    then:
    getText() == "No response sent for GET request to / (last handler: closure at line 34 of NoResponseSentDetectionSpec.groovy)"
    response.statusCode == 500
  }

  def "sends empty 500 when promise is unsubscribed"() {
    when:
    handlers {
      get {
        // Missing .then() to actually subscribe to the promise
        Promise.async { it.success("foo") }
      }
    }

    then:
    getText() == "No response sent for GET request to / (last handler: closure at line 49 of NoResponseSentDetectionSpec.groovy)"
    response.statusCode == 500
  }

  static class MyHandler implements Handler {
    @Override
    void handle(Context context) throws Exception {

    }
  }

  def "message contains custom class name"() {
    when:
    handlers {
      all new MyHandler()
    }

    then:
    getText() == "No response sent for GET request to / (last handler: ratpack.handling.NoResponseSentDetectionSpec\$MyHandler)"
    response.statusCode == 500
  }

  def "message contains custom inner class name"() {
    when:
    handlers {
      all new Handler() {
        @Override
        void handle(Context context) throws Exception {
          def foo = "bar" // need some code to get line number
        }
      }
    }

    then:
    getText() == "No response sent for GET request to / (last handler: anonymous class ratpack.handling.NoResponseSentDetectionSpec\$1 at approximately line 82 of NoResponseSentDetectionSpec.groovy)"
    response.statusCode == 500
  }

  def "sends error response with large request body"() {
    when:
    handlers {
      post {
        // no response sent here, no next() called
      }
    }

    then:
    def r = request {
      it.post().body.text("1" * 10000)
    }

    r.body.text == "No response sent for POST request to / (last handler: closure at line 95 of NoResponseSentDetectionSpec.groovy)"
    response.statusCode == 500
  }

  def "sends error response with too large request body"() {
    when:
    handlers {
      post {
        request.maxContentLength = 10
        // no response sent here, no next() called
      }
    }

    and:
    request { it.post().body.text("1" * 10000) }

    then:
    response.status == Status.PAYLOAD_TOO_LARGE
  }
}
