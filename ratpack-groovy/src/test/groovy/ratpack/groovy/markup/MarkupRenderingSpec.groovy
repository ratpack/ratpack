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

package ratpack.groovy.markup

import ratpack.http.internal.DefaultMediaType
import ratpack.test.internal.RatpackGroovyAppSpec

import static ratpack.groovy.Groovy.htmlBuilder
import static io.netty.handler.codec.http.HttpHeaders.Names.*


class MarkupRenderingSpec extends RatpackGroovyAppSpec {

  def "can render html markup"() {
    when:
    handlers {
      get {
        render htmlBuilder {
          html {
            head {}
            body {
              p "Hello!"
            }
          }
        }
      }
    }

    then:
    text == """<html>
  <head />
  <body>
    <p>Hello!</p>
  </body>
</html>"""

    response.headers.get(CONTENT_TYPE) == new DefaultMediaType("text/html", "UTF-8").toString()
  }
}
