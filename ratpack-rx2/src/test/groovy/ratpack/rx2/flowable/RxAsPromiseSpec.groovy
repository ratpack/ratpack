/*
 * Copyright 2017 the original author or authors.
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

package ratpack.rx2.flowable

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import ratpack.exec.Blocking
import ratpack.exec.Operation
import ratpack.rx2.RxRatpack
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification

class RxAsPromiseSpec extends Specification {

  @AutoCleanup
  ExecHarness harness = ExecHarness.harness()
  AsyncService service = new AsyncService()

  static class AsyncService {
    int counter = 0

    public Flowable<Void> fail() {
      RxRatpack.flow(Blocking.get { throw new RuntimeException("!!!") }, BackpressureStrategy.BUFFER)
    }

    public <T> Flowable<T> flow(T value) {
      RxRatpack.flow(Blocking.get { value }, BackpressureStrategy.BUFFER)
    }

    public Flowable<Void> increment() {
      RxRatpack.flow(Operation.of { counter++ }, BackpressureStrategy.BUFFER)
    }
  }

  def setup() {
    RxRatpack.initialize()
  }

  def "can test async service"() {
    when:
    def result = harness.yield { service.flow("foo").promise() }

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
    def result = harness.yield { service.flow("foo").promiseSingle() }

    then:
    result.valueOrThrow == "foo"
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
