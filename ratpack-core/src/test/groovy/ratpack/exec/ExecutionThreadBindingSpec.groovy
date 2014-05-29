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

package ratpack.exec

import ratpack.test.internal.RatpackGroovyDslSpec

class ExecutionThreadBindingSpec extends RatpackGroovyDslSpec {

  def "context is bound for promise fulfillment"() {
    when:
    handlers {
      get { ExecController execController ->
        promise { f ->
          assert execController.execution
          Thread.start {
            assert !execController.managedThread
            f.success('foo')
          }
        } then {
          assert execController.managedThread
          render it
        }
      }
    }

    then:
    text == "foo"
  }
}
