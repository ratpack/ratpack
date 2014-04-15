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

package ratpack.rx

import ratpack.error.DebugErrorHandler
import ratpack.error.ServerErrorHandler
import ratpack.test.internal.RatpackGroovyDslSpec
import rx.functions.Action1

import static ratpack.registry.Registries.just

class RxRatpackSpec extends RatpackGroovyDslSpec {

  def "can use error handler"() {
    given:
    def e = new Exception("!")

    when:
    handlers {
      register just(ServerErrorHandler, new DebugErrorHandler())
      get {
        rx.Observable.<String> error(e).subscribe(new Action1<String>() {
          @Override
          void call(String str) {
            render "shouldn't happen"
          }
        }, RxRatpack.errorHandler(context))
      }
    }

    then:
    text.startsWith e.toString()
  }
}
