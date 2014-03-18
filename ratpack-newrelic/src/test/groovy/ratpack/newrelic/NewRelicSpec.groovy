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

package ratpack.newrelic

import ratpack.test.internal.RatpackGroovyDslSpec

class NewRelicSpec extends RatpackGroovyDslSpec {

  def "new relic interceptor executes"() {
    when:
    modules << new NewRelicModule()
    handlers {
      get {
        render "ok"
      }
    }

    then:
    text == "ok"
  }

}
