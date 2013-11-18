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

package ratpack.reactor

import ratpack.handling.Context
import ratpack.test.internal.RatpackGroovyDslSpec
import reactor.core.Environment
import reactor.core.Reactor
import reactor.core.spec.Reactors
import reactor.event.Event
import reactor.function.Consumer

import static reactor.core.composable.spec.Promises.defer
import static reactor.event.selector.Selectors.$

class ReactorSpec extends RatpackGroovyDslSpec {

  def "can use reactor"() {
    when:
    app {
      modules {
        def env = new Environment()
        bind env
        bind Reactor, env.rootReactor
      }

      handlers {
        get(":value") { Environment env ->
          def deferred = defer(env)
          Context context = delegate

          deferred.
            compose().
            onSuccess(new Consumer<Integer>() {
              void accept(Integer o) {
                context.response.send o.toString()
              }
            }).
            onError(new Consumer<Throwable>() {
              void accept(Throwable o) {
                context.error new Exception(o)
              }
            })

          deferred.accept(4)

        }
      }
    }

    then:
    getText("2") == "4"
  }


}
