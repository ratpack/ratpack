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

class AsyncIntegrationSpec extends RatpackGroovyDslSpec {

  def "async api that uses own threads can integrate via promise"() {
    when:
    handlers {
      get { ExecController execController ->
        Promise.async { f ->
          Thread.start {
            assert !Execution.isManagedThread()
            f.success('foo')
          }
        } then {
          assert Execution.isManagedThread()
          render it
        }
      }
    }

    then:
    text == "foo"
  }

  def "async api that produces exception on own threads can integrate via promise"() {
    when:
    handlers {
      get { ExecController execController ->
        Promise.async { f ->
          Thread.start {
            assert !Execution.isManagedThread()
            f.error(new Exception("!"))
          }
        } onError {
          assert Execution.isManagedThread()
          render it.message
        } then {
          render "shouldn't happen"
        }
      }
    }

    then:
    text == "!"
  }

}
