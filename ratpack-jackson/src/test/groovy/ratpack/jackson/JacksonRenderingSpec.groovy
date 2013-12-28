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

package ratpack.jackson

import ratpack.test.internal.RatpackGroovyDslSpec

import static Jackson.json

class JacksonRenderingSpec extends RatpackGroovyDslSpec {

  static class User {
    String username
    String password
  }

  def setup() {
    modules << new JacksonModule()
  }

  def "can render custom objects as json"() {
    when:
    handlers {
      get {
        render json(new User(username: "foo", password: "bar"))
      }
    }

    then:
    text == '{"username":"foo","password":"bar"}'
  }

  def "can render standard objects as json"() {
    when:
    handlers {
      get {
        render json(username: "foo", numbers: [1, 2, 3])
      }
    }

    then:
    text == '{"username":"foo","numbers":[1,2,3]}'
  }

  def "can pretty print"() {
    def lf = System.getProperty("line.separator")
    def indent = "  "
    def prettyString = '{' + lf +
      indent + '"username" : "foo",' + lf +
      indent + '"password" : "bar"' + lf +
      '}'

    when:
    modules {
      get(JacksonModule).prettyPrint = true
    }
    handlers {
      get {
        render json(new User(username: "foo", password: "bar"))
      }
    }

    then:
    text == prettyString

  }

}
