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

import ratpack.handling.Context
import ratpack.registry.RegistrySpec
import ratpack.test.exec.ExecHarness
import ratpack.test.internal.BaseRatpackSpec
import spock.lang.AutoCleanup

class OperationSpec extends BaseRatpackSpec {

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

  def "An operation that needs execution context"() {

    given: "a context"
    Context context = Mock {
      1 * get(Integer) >> 1
    }
    Closure<RegistrySpec> registry = { RegistrySpec registrySpec ->
      registrySpec.add(context)
    }

    when: "executing with the registry"
    exec.executeSingle(registry) { Execution execution ->
      async(execution.current().get(Context).get(Integer))
        .next { events << it }
        .operation()
    }

    then:
    events == [1]
  }

  def "can wiretap successful operation"() {
    when:
    exec.execute {
      Operation.of { 1 }
        .wiretap { events << it }
    }

    then:
    events[0] == Optional.empty()
  }

  def "can wiretap failed operation"() {
    when:
    exec.execute {
      Operation.error(new RuntimeException())
        .wiretap { events << it }
    }

    then:
    def e = thrown(RuntimeException)
    (events[0] as Optional<Throwable>).get().is(e)
  }

  def "wiretap of successful operation can fail"() {
    when:
    exec.execute {
      Operation.of { 1 }
        .wiretap {
          throw new RuntimeException("!")
        }
    }

    then:
    def e = thrown(RuntimeException)
    e.message == "!"
    e.suppressed.length == 0
  }

  def "wiretap of failed operation can fail"() {
    when:
    exec.execute {
      Operation.error(new RuntimeException())
        .wiretap {
          throw new RuntimeException("!")
        }
    }

    then:
    def e = thrown(RuntimeException)
    e.message == "!"
    e.suppressed.length == 0
  }

  private <T> Promise<T> async(T t) {
    Promise.async { f -> Thread.start { f.success(t) } }
  }

}
