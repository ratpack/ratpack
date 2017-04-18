/*
 * Copyright 2016 the original author or authors.
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

package ratpack.stream

import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import ratpack.exec.Promise
import ratpack.func.Action
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.lang.Timeout

import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier

@Timeout(180)
class StreamForkSpec extends Specification {

  @AutoCleanup
  def harness = ExecHarness.harness()

  def "can fork publisher"() {
    when:
    def barrier = new CyclicBarrier(2)
    def p = Streams.yield {
      if (it.requestNum < 10) {
        it.requestNum
      } else {
        barrier.await()
        null
      }
    }

    def v = harness.yield {
      Promise.async { down ->
        p.fork().subscribe(new Subscriber() {
          @Override
          void onSubscribe(Subscription s) {
            s.request(Long.MAX_VALUE)
          }

          @Override
          void onNext(Object o) {
            if (o == 9) {
              barrier.await()
            }
          }

          @Override
          void onError(Throwable t) {
            down.error(t)
          }

          @Override
          void onComplete() {
            down.success(10)
          }
        })
      }
    }.valueOrThrow

    then:
    v == 10
  }

  def "can cancel forked publisher"() {
    when:
    def barrier = new CyclicBarrier(2)
    def latch = new CountDownLatch(5)

    def p = Streams.yield {
      if (it.requestNum < 10) {
        it.requestNum
      } else {
        barrier.await()
        null
      }
    }

    def dispose = {
      latch.countDown()
    } as Action
    def v = harness.yield {
      Promise.async { down ->
        p.fork({}, dispose).subscribe(new Subscriber() {
          Subscription subscription

          @Override
          void onSubscribe(Subscription s) {
            subscription = s
            s.request(Long.MAX_VALUE)
          }

          @Override
          void onNext(Object o) {
            if (o == 4) {
              barrier.await()
              subscription.cancel()
              down.success(10)
            }
          }

          @Override
          void onError(Throwable t) {
            down.error(t)
          }

          @Override
          void onComplete() {
            down.error(new Exception("!"))
          }
        })
      }
    }.valueOrThrow

    then:
    latch.await()
    v == 10
  }

}
