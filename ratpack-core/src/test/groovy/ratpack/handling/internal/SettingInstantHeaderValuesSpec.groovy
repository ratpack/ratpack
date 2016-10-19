/*
 * Copyright 2016 the original author or authors.
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

package ratpack.handling.internal

import ratpack.test.internal.RatpackGroovyDslSpec

import java.time.Instant

class SettingInstantHeaderValuesSpec extends RatpackGroovyDslSpec {

  def "can set instant headers"() {
    when:
    def now = Instant.now()
    def nowFloored = Instant.ofEpochSecond(now.epochSecond)

    handlers {
      get {
        response.headers.set "h1", now
        response.headers.set "h2", [now, "a"]
        response.headers.add "h3", now
        render "ok"
      }
    }

    then:
    def headers = get().headers
    headers.getInstant("h1") == nowFloored
    headers.getInstant("h2") == nowFloored
    headers.getInstant("h3") == nowFloored
  }

}
