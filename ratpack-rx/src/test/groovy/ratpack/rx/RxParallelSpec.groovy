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

import ratpack.exec.internal.DefaultExecController
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier

import static ratpack.rx.RxRatpack.forkOnNext
import static ratpack.rx.RxRatpack.observe

class RxParallelSpec extends Specification {

  @AutoCleanup
  def controller = new DefaultExecController(12)
  def control = controller.control

  def setup() {
    RxRatpack.initialize()
  }

  def "can use scheduler to observe in parallel"() {
    given:
    def barrier = new CyclicBarrier(11)
    def received = [].asSynchronized()

    when:
    controller.start {
      rx.Observable.from((0..9).toList())
        .parallel { it.map { received << it; barrier.await(); it } }
        .subscribe()
    }
    barrier.await()

    then:
    received.sort() == (0..9).toList()
  }

  def "when using scheduler can use ratpack async api"() {
    given:
    def barrier = new CyclicBarrier(11)
    def received = [].asSynchronized()

    when:
    controller.start {

      rx.Observable.from((0..9).toList())
        .parallel { i ->
        i.flatMap { n ->
          observe(control.blocking { it }).map {
            received << n
            barrier.await()
          }
        }
      }
      .subscribe()
    }
    barrier.await()


    then:
    received.sort() == (0..9).toList()
  }

  def "can use fork then join to perform parallel tasks"() {
    def latch = new CountDownLatch(1)
    List<Integer> nums = []

    when:
    controller.start {

      def o = rx.Observable.from(1, 2, 3, 4, 5)
        .parallel {
        it.flatMap { n ->
          observe control.promise { f ->
            Thread.start {
              f.success(n * 2)
            }
          }
        }
      }

      RxRatpack.forkAndJoin(it, o).toList().subscribe {
        nums = it
      }

      it.onComplete {
        latch.countDown()
      }
    }

    then:
    latch.await()
    nums.sort() == [2, 4, 6, 8, 10]
  }

  def "can use fork on next to fan out"() {
    given:
    def sequence = rx.Observable.from(1, 2, 3, 4, 5)
    def barrier = new CyclicBarrier(6)
    def received = [].asSynchronized()

    when:
    sequence.lift(forkOnNext(control)).subscribe {
      received << it
      barrier.await()
    }
    barrier.await()

    then:
    received.sort() == [1, 2, 3, 4, 5]
  }

  def "errors are collected when using fork on next"() {
    given:
    def sequence = rx.Observable.from(1, 2, 3, 4, 5)
    def barrier = new CyclicBarrier(2)
    Throwable e = null

    when:
    sequence.lift(forkOnNext(control)).subscribe({
      throw new RuntimeException("!")
    }, { e = it; barrier.await() })
    barrier.await()

    then:
    e.message == "!"
  }

}
