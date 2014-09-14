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
import spock.lang.Issue

import static io.netty.handler.codec.http.HttpResponseStatus.OK

@Issue("https://github.com/ratpack/ratpack/issues/127")
class SpecifiedContentTypeSpec extends RatpackGroovyDslSpec {

  def "content type can be specified by handler"() {
    given:
    handlers {
      get("path") {
        response.contentType mimeType
        response.send content
      }
    }

    when:
    get("path")

    then:
    with(response) {
      statusCode == OK.code()
      body.text == content
      headers.get("Content-Type") == mimeType
    }

    where:
    content | mimeType
    "x,y,z" | "text/csv"
  }

}
