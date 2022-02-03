/*
 * Copyright 2022 the original author or authors.
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
package ratpack.core.handling

import ratpack.core.http.client.RequestSpec
import ratpack.test.internal.RatpackGroovyDslSpec

class RedirectSpec extends RatpackGroovyDslSpec {

  @Override
  void configureRequest(RequestSpec requestSpec) {
    requestSpec.redirects 0
  }

  def "redirect #val.class.name #val"() {
    when:
    handlers {
      get {
        redirect(val)
      }
    }

    then:
    def resp = get("")
    resp.statusCode == 302
    resp.headers.get("Location") == location

    where:
    val                                 | location
    "http://www.google.com"             | "http://www.google.com"
    "/"                                 | "/"
    "//www.google.com"                  | "//www.google.com"
    URI.create("http://www.google.com") | "http://www.google.com"
  }


}
