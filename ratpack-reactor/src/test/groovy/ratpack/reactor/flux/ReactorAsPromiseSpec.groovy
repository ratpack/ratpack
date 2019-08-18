/*
 * Copyright 2018 the original author or authors.
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

package ratpack.reactor.flux

import ratpack.exec.Blocking
import ratpack.exec.Operation
import ratpack.reactor.ReactorRatpack
import ratpack.test.exec.ExecHarness
import reactor.core.publisher.Flux
import spock.lang.AutoCleanup
import spock.lang.Specification

class ReactorAsPromiseSpec extends Specification {

  @AutoCleanup
  ExecHarness harness = ExecHarness.harness()
  AsyncService service = new AsyncService()

  static class AsyncService {
    int counter = 0

    public Flux<Void> fail() {
      ReactorRatpack.flux(Blocking.get { throw new RuntimeException("!!!") })
    }

    public <T> Flux<T> flux(T value) {
      ReactorRatpack.flux(Blocking.get { value })
    }

    public Flux<Void> increment() {
      ReactorRatpack.flux(Operation.of { counter++ })
    }
  }

  def setup() {
    ReactorRatpack.initialize()
  }

  def "can test async service"() {
    when:
    def result = harness.yield { service.flux("foo").promise() }

    then:
    result.valueOrThrow == ["foo"]
  }

  def "failed observable causes exception to be thrown"() {
    when:
    harness.yield { service.fail().promise() }.valueOrThrow

    then:
    def e = thrown RuntimeException
    e.message == "!!!"
  }

  def "can unpack single"() {
    when:
    def result = harness.yield { service.flux("foo").promise() }

    then:
    result.valueOrThrow == ["foo"]
  }

  def "can observe operation"() {
    given:
    def nexted = false

    when:
    harness.run {
      service.increment().subscribe {
        nexted = true
      }
    }

    then:
    noExceptionThrown()
    service.counter == 1
    !nexted

  }

}
