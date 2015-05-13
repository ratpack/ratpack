/*
 * Copyright 2015 the original author or authors.
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

package ratpack.handling

import ratpack.test.internal.RatpackGroovyDslSpec

class HandlerDecorationSpec extends RatpackGroovyDslSpec {

  def "decorators are applied LIFO"() {
    given:
    def events = []

    when:
    bindings {
      multiBindInstance HandlerDecorator.prepend { events << "1"; it.next() }
      multiBindInstance HandlerDecorator.prepend { events << "2"; it.next() }
    }
    handlers {
      get { render "ok" }
    }

    then:
    text == "ok"
    events == ["2", "1"]
  }
}
