/*
 * Copyright 2013 the original author or authors.
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

import ratpack.http.client.RequestSpec
import ratpack.test.internal.RatpackGroovyDslSpec

class RequestBodyReadingSpec extends RatpackGroovyDslSpec {

  def "can get request body as bytes"() {
    when:
    handlers {
      post {
        response.send new String(request.body.bytes, "utf8")
      }
    }

    then:
    requestSpec { RequestSpec requestSpec -> requestSpec.body.stream({ it << "foo" }) }
    postText() == "foo"
  }

  def "can get request body as input stream"() {
    when:
    handlers {
      post {
        response.send new String(request.body.inputStream.bytes, "utf8")
      }
    }

    then:
    requestSpec { it.body.stream { it << "foo".getBytes("utf8") } }
    postText() == "foo"
  }

  def "can get large request body as bytes"() {
    given:
    def string = "a" * 1024 * 9

    when:
    handlers {
      post {
        response.send new String(request.body.bytes, "utf8")
      }
    }

    then:
    requestSpec { requestSpec ->
      requestSpec.body.stream({ it << string.getBytes("utf8") })
      postText() == string
    }
  }

  def "get bytes on get request"() {
    when:
    handlers {
      all {
        response.send request.body.bytes.length.toString()
      }
    }

    then:
    getText() == "0"
    postText() == "0"
    putText() == "0"
  }

}
