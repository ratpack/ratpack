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

package ratpack.rx2

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.SingleOnSubscribe
import io.reactivex.annotations.NonNull
import ratpack.exec.Blocking
import ratpack.exec.Operation
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification

class RxAsPromiseSpec extends Specification {

  @AutoCleanup
  ExecHarness harness = ExecHarness.harness()
  AsyncService service = new AsyncService()

  static class AsyncService {
    int counter = 0

    public Single<Void> fail() {
      RxRatpack.single(Blocking.get { throw new RuntimeException("!!!") })
    }

    public <T> Single<T> single(T value) {
      RxRatpack.single(Blocking.get { value })
    }

    public SingleOnSubscribe<String> singleOnSubscribe(String value) {
      new SingleOnSubscribe<String>() {
        @Override
        void subscribe(@NonNull SingleEmitter<String> emitter) throws Exception {
          emitter.onSuccess(value)
        }
      }
    }


    public <T> Observable<T> observe(T... values) {
      RxRatpack.observe(Blocking.get { values.toList() })
    }

    public ObservableOnSubscribe<String> observableOnSubscribe(String value) {
      new ObservableOnSubscribe<String>() {
        @Override
        public void subscribe(ObservableEmitter<String> subscriber) throws Exception {
          subscriber.onNext(value)
          subscriber.onComplete()
        }
      }
    }

    public Completable increment() {
      RxRatpack.complete(Operation.of { counter++ })
    }
  }

  def setup() {
    RxRatpack.initialize()
  }

  def "can unpack single"() {
    when:
    def result = harness.yield { service.single("foo").promise() }

    then:
    result.valueOrThrow == "foo"
  }

  def "can unpack singleOnSubscribe"() {
    when:
    def result = harness.yield { service.singleOnSubscribe("foo").promise() }

    then:
    result.valueOrThrow == "foo"
  }

  def "can unpack observable"() {
    when:
    def result = harness.yield { service.observe("foo", "bar").promiseAll() }

    then:
    result.valueOrThrow == ["foo", "bar"]
  }

  def "can unpack observableOnSubscribe"() {
    when:
    def result = harness.yield { service.observableOnSubscribe("foo").promiseAll() }

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

  def "can complete operation"() {
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
    nexted

  }

}
