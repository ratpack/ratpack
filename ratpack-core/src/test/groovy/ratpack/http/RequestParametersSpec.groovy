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

import ratpack.test.internal.RatpackGroovyDslSpec

class RequestParametersSpec extends RatpackGroovyDslSpec {

  def "can get query params"() {
    when:
    app {
      handlers {
        get {
          response.send request.queryParams.toString()
        }
      }
    }

    then:
    getText() == "[:]" && resetRequest()
    getText("?a=b") == "[a:[b]]" && resetRequest()
    request.with {
      queryParam "a", "b", "c"
      queryParam "d", "e"
    }
    getText() == "[a:[b, c], d:[e]]" && resetRequest()
    getText("?abc") == "[abc:[]]" && resetRequest()
  }

  def "can get form params"() {
    when:
    app {
      handlers {
        post {
          render request.form.toString()
        }
      }
    }

    then:
    postText() == "[:]" && resetRequest()
    request.with {
      param "a", "b"
    }
    postText() == "[a:[b]]" && resetRequest()
    request.with {
      param "a", "b", "c"
      param "d", "e"
      param "abc"
    }
    postText() == "[a:[b, c], d:[e], abc:[]]" && resetRequest()
  }
}
