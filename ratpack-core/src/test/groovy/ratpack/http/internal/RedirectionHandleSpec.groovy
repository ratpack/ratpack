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

package ratpack.http.internal

import ratpack.handling.Handlers
import ratpack.test.internal.RatpackGroovyDslSpec

class RedirectionHandleSpec extends RatpackGroovyDslSpec {

  def "ok for valid"() {
    given:
    handlers {
      handler(Handlers.redirect('http://www.ratpack.io', 310))
    }

    when:
    get()

    then:
    response.statusCode == 310
    response.header('Location') == 'http://www.ratpack.io'
  }

  def "it checks that the desired status code is a 3XX one"() {
    given:
    handlers {
      handler(Handlers.redirect('http://www.ratpack.io', statusCode))
    }

    when:
    get()

    then:
    response.statusCode == 500

    where:
    statusCode << [299, 400]

  }

}