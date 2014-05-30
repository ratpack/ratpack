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

import ratpack.exec.ExecController
import ratpack.test.internal.RatpackGroovyDslSpec
import reactor.core.Environment
import reactor.core.Reactor

import static reactor.core.composable.spec.Promises.defer

class ReactorSpec extends RatpackGroovyDslSpec {

  def "can use reactor"() {
    when:
    bindings {
      def env = new Environment()
      bind env
      bind Reactor, env.rootReactor
    }

    handlers {
      get(":value") { ExecController execController, Environment env ->

        // Create a reactor deferred
        def deferred = defer(env)

        // Bridge the reactor deferred so that the value is consumed in the Ratpack event loop and execution is rejoined
        RatpackReactor.consume(context, deferred) then {
          render it
        }

        // Supply the value asynchronously
        execController.executor.submit {
          deferred.accept(pathTokens.value)
        }
      }
    }

    then:
    getText("2") == "2"
  }


}
