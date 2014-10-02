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

package ratpack.error

import ratpack.test.internal.RatpackGroovyDslSpec

class ClientErrorHandler404Spec extends RatpackGroovyDslSpec {

  def "client error handler registered at top level is used for 404"() {
    def clientErrorHandler = { ctx, code -> ctx.render("here!") } as ClientErrorHandler

    when:
    handlers {
      register { add(ClientErrorHandler, clientErrorHandler) }
    }

    then:
    getText() == "here!"
  }
}
