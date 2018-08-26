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

import com.google.common.base.StandardSystemProperty
import com.google.gson.GsonBuilder
import ratpack.stream.Streams
import ratpack.test.internal.RatpackGroovyDslSpec

import java.time.Duration

import static Gson.chunkedJsonList
import static Gson.json

class GsonRenderingSpec extends RatpackGroovyDslSpec {

  static class User {
    String username
    String password
  }

  def setup() {
    bindings {
      module(GsonModule)
    }
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
    def lf = StandardSystemProperty.LINE_SEPARATOR.value()
    def indent = "  "
    def prettyString = '{' + lf +
      indent + '"username": "foo",' + lf +
      indent + '"password": "bar"' + lf +
      '}'

    when:
    bindings {
      module(GsonModule)
      bindInstance(new GsonBuilder().setPrettyPrinting().create())
    }
    handlers {
      get {
        render json(new User(username: "foo", password: "bar"))
      }
    }

    then:
    text == prettyString
  }

  def "can stream list"() {
    when:
    handlers {
      get {
        render chunkedJsonList(context, Streams.publish([1, 2, [foo: "bar"], 4]))
      }
    }

    then:
    text == '[1,2,{"foo":"bar"},4]'
  }

  def "can periodically stream list"() {
    when:
    handlers {
      get { ctx ->
        def data = [1, 2, [foo: "bar"], 4]
        render chunkedJsonList(context, Streams.periodically(ctx, Duration.ofMillis(100), {
          it < data.size() ? data.get(it) : null
        }))
      }
    }

    then:
    text == '[1,2,{"foo":"bar"},4]'
  }

  def "can stream large list"() {
    List<String> data = ["a" * 5000] * 100

    when:
    handlers {
      get {
        render chunkedJsonList(context, Streams.publish(data))
      }
    }

    then:
    text == "[" + data.collect { "\"$it\"" }.join(",") + "]"
  }

  static class Views {
    static class Public {}

    static class Secret extends Public {}
  }
}
