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

package ratpack.exec

import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification

class OperationSpec extends Specification {

  @AutoCleanup
  def exec = ExecHarness.harness()
  def events = []

  def "can use operation"() {
    when:
    exec.execute {
      Operation.of {
        async(1).wiretap { events << 1 }.then {}
      }.next {
        events << 2
      }.next(
        async(1).wiretap { events << 3 }.operation()
      ).next {
        events << 4
      }
    }

    then:
    noExceptionThrown()
    events == [1, 2, 3, 4]
  }

  private <T> Promise<T> async(T t) {
    Promise.async { f -> Thread.start { f.success(t) } }
  }

}
