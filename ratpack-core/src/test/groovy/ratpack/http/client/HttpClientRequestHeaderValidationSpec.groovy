/*
 * Copyright 2019 the original author or authors.
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

class HttpClientRequestHeaderValidationSpec extends BaseHttpClientSpec {

  def "invalid header values yield exception"() {
    when:
    otherApp {
      all { render "ok" }
    }

    handlers {
      all { HttpClient httpClient ->
        httpClient.get(otherApp.address) {
          header("test", "value\r\nanother-header: another value")
        } onError {
          render it.toString()
        } then {
          render it.body.text
        }
      }
    }

    then:
    def response = get()
    response.headers.names == ['content-type', 'content-length'].toSet()
    response.body.text == "java.lang.IllegalArgumentException: only ' ' and '\\t' are allowed after '\\n': value\r\nanother-header: another value"
  }

}
