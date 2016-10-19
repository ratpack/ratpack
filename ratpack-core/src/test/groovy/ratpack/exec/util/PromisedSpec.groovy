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

package ratpack.exec.util

import ratpack.exec.Execution
import ratpack.exec.Promise
import ratpack.func.Action
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.util.concurrent.BlockingVariables

class PromisedSpec extends Specification {

  @AutoCleanup
  def harness = ExecHarness.harness()
  def blocking = new BlockingVariables()

  def "can complete with success value"() {
    when:
    harness.run({
      def v = new Promised()

      Execution.fork().start { v.promise().then { blocking.a = it } }
      Execution.fork().start { v.promise().then { blocking.b = it } }
      Execution.fork().start { v.promise().then { blocking.c = it } }

      Execution.fork().start { v.success(1) }

    } as Action)

    then:
    blocking.a == 1
    blocking.b == 1
    blocking.c == 1
  }

  def "can create promise after completed"() {
    when:
    harness.run({
      def v = new Promised()
      v.success(1)

      Execution.fork().start { v.promise().then { blocking.a = it } }
      Execution.fork().start { v.promise().then { blocking.b = it } }
      Execution.fork().start { v.promise().then { blocking.c = it } }

    } as Action)

    then:
    blocking.a == 1
    blocking.b == 1
    blocking.c == 1
  }

  def "can error"() {
    when:
    harness.run({
      def v = new Promised()

      Execution.fork().start { v.promise().onError { blocking.a = it }.then { blocking.a = it } }
      Execution.fork().start { v.promise().onError { blocking.b = it }.then { blocking.b = it } }
      Execution.fork().start { v.promise().onError { blocking.c = it }.then { blocking.c = it } }

      Execution.fork().start { v.error(new Error("!")) }
    } as Action)

    then:
    blocking.a instanceof Error
    blocking.b instanceof Error
    blocking.c instanceof Error
  }

  def "can connect to promise"() {
    when:
    harness.run({
      def v = new Promised()

      Execution.fork().start { v.promise().then { blocking.a = it } }
      Execution.fork().start { v.promise().then { blocking.b = it } }
      Execution.fork().start { v.promise().then { blocking.c = it } }

      Execution.fork().start {
        Promise.async { it.success(2) }.transform { it.connect(v) }
      }
    } as Action)

    then:
    blocking.a == 2
    blocking.b == 2
    blocking.c == 2
  }

  def "throws when already supplied"() {
    when:
    harness.run({
      def v = new Promised()
      v.success(1)
      v.success(2)
    } as Action)

    then:
    thrown Promised.AlreadySuppliedException
  }
}
