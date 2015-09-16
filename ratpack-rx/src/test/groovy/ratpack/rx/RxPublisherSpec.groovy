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

package ratpack.rx

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import ratpack.exec.Blocking
import ratpack.test.exec.ExecHarness
import rx.Observable
import spock.lang.AutoCleanup
import spock.lang.Specification


class RxPublisherSpec extends Specification {

  @AutoCleanup
  ExecHarness harness = ExecHarness.harness()
  AsyncService service = new AsyncService()

  static class AsyncService {
    public Observable<Void> fail() {
      RxRatpack.observe(Blocking.get { throw new RuntimeException("!!!") })
    }

    public <T> Observable<T> observe(T value) {
      RxRatpack.observe(Blocking.get { value })
    }
  }

  def setup() {
    RxRatpack.initialize()
  }

  def "convert RX Observable to ReactiveStreams Publisher"() {
    given:
    Publisher<String> pub = service.observe("foo").publisher()
    def received = []
    Subscription subscription
    boolean complete = false

    when:
    harness.run {
      pub.subscribe(new Subscriber<String>() {
        @Override
        void onSubscribe(Subscription s) {
          subscription = s
        }

        @Override
        void onNext(String s) {
          received << s
        }

        @Override
        void onError(Throwable t) {
          received << t
        }

        @Override
        void onComplete() {
          complete = true
        }
      })
      subscription.request(1)
    }

    then:
    received.size() == 1
    received.first() == 'foo'
    complete
  }

}
