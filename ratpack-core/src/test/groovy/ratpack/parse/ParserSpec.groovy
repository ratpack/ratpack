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

package ratpack.parse

import com.google.common.reflect.TypeToken
import ratpack.handling.Context
import ratpack.http.TypedData
import ratpack.test.internal.RatpackGroovyDslSpec

class ParserSpec extends RatpackGroovyDslSpec {

  static class IntParser extends NoOptParserSupport {
    @Override
    <T> T parse(Context context, TypedData body, TypeToken<T> type) throws Exception {
      if (type.rawType == Integer) {
        body.text.toInteger()
      } else {
        return null
      }
    }
  }

  def "can use parsers"() {
    when:
    bindings {
      bind IntParser
    }
    handlers {
      post {
        def i = parse Integer
        i.then { integer ->
          response.send(integer.getClass().toString())
        }
      }
    }

    then:
    requestSpec { it.body.stream { it << "123" } }
    postText() == Integer.toString()
  }

}
