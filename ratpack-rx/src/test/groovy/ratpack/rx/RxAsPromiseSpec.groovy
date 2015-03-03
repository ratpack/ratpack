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

import ratpack.exec.ExecControl
import ratpack.test.exec.ExecHarness
import rx.Observable
import spock.lang.AutoCleanup
import spock.lang.Specification

class RxAsPromiseSpec extends Specification {

  @AutoCleanup
  ExecHarness harness = ExecHarness.harness()
  AsyncService service = new AsyncService(execControl: harness)

  static class AsyncService {
    ExecControl execControl

    public Observable<Void> fail() {
      RxRatpack.observe(execControl.blocking { throw new RuntimeException("!!!") })
    }

    public <T> Observable<T> observe(T value) {
      RxRatpack.observe(execControl.blocking { value })
    }
  }

  def setup() {
    RxRatpack.initialize()
  }

  def "can test async service"() {
    when:
    def result = harness.yield { service.observe("foo").asPromise() }

    then:
    result.valueOrThrow == ["foo"]
  }

  def "failed observable causes exception to be thrown"() {
    when:
    harness.yield { service.fail().asPromise() }.valueOrThrow

    then:
    def e = thrown RuntimeException
    e.message == "!!!"
  }

  def "can unpack single"() {
    when:
    def result = harness.yield { service.observe("foo").asPromiseSingle() }

    then:
    result.valueOrThrow == "foo"
  }

}
