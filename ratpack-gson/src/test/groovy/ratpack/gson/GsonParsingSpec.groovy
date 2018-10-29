/*
 * Copyright 2018 the original author or authors.
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

package ratpack.gson

import com.google.common.reflect.TypeToken
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import ratpack.http.client.RequestSpec
import ratpack.test.internal.RatpackGroovyDslSpec

import static Gson.jsonElement

class GsonParsingSpec extends RatpackGroovyDslSpec {

  def setup() {
    bindings {
      module(GsonModule)
    }
  }

  def "can parse json node"() {
    when:
    handlers {
      post {
        def node = parse jsonElement()
        node.then {
          response.send it.get("value").toString()
        }
      }
    }

    and:
    requestSpec { RequestSpec requestSpec ->
      requestSpec.body.stream({ it << /{"value": 3}/ })
      requestSpec.body.type("application/json")
    }

    then:
    postText() == "3"
  }

  def "can parse #requestBody leniently"() {
    given:
    bindings {
      module(GsonModule)
      bindInstance(new GsonBuilder().setLenient().create())
    }

    and:
    handlers {
      post {
        def node = parse jsonElement()
        node.then {
          response.send it.get("value").toString()
        }
      }
    }

    and:
    requestSpec { RequestSpec requestSpec ->
      requestSpec.body.stream({ it << requestBody })
      requestSpec.body.type("application/json")
    }

    expect:
    postText() == "3"

    where:
    requestBody << [/{'value': 3}/, /{value: 3}/]
  }

  static class Pogo {
    String value
    def foo = [:]
  }

  def "can parse json as #classType"() {
    when:
    Class returnedType

    handlers {
      post {
        def object = parse classType
        object.then {
          returnedType = it.getClass()
          render "${it.value}:${it.foo?.value}"
        }
      }
    }

    and:
    requestSpec { RequestSpec requestSpec ->
      requestSpec.body.type("application/json")
      requestSpec.body.stream({ it << /{"value": 1, "foo": {"value": 2}}/ })
    }

    then:
    postText() == renderText
    classType.isAssignableFrom(returnedType)

    where:
    classType || renderText
    HashMap || "1.0:2.0"
    Map || "1.0:2.0"
    Object || "1.0:2.0"
    Pogo || "1:2.0"
    JsonElement || "1:2"
  }

  def "can parse generic type"() {
    when:
    handlers {
      post {
        def body = parse(new TypeToken<List<Integer>>() {})
        body.then { b ->
          render b.collect { it.getClass().name }.toString()
        }
      }
    }

    and:
    requestSpec { RequestSpec requestSpec ->
      requestSpec.body.type("application/json")
      requestSpec.body.stream({ it << /[1]/ })
    }

    then:
    postText() == "[java.lang.Integer]"
  }

  def "can parse custom type with +json suffix as json node"() {
    when:
    handlers {
      post {
        def node = parse jsonElement()
        node.then {
          response.send it.get("value").toString()
        }
      }
    }

    and:
    requestSpec {
      it.body.text('{"value": 3}').type("application/vnd.foo+json")
    }

    then:
    postText() == "3"
  }

  def "can parse custom type with +json suffix as object"() {
    when:
    handlers {
      post {
        parse Map then {
          response.send it.value.toString()
        }
      }
    }

    and:
    requestSpec {
      it.body.text('{"value": 3}').type("application/vnd.foo+json")
    }

    then:
    postText() == "3.0"
  }
}
