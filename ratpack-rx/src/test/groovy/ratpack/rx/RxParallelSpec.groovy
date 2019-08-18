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

import ratpack.exec.Execution
import ratpack.registry.RegistrySpec
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Ignore
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit

import static ratpack.rx.RxRatpack.observe

class RxParallelSpec extends Specification {

  @AutoCleanup
  def harness = ExecHarness.harness(12)

  def setup() {
    RxRatpack.initialize()
  }

  @Ignore("Waiting for release of RxJavaParallel")
  def "can use scheduler to observe in parallel"() {
    given:
    def latch = new CountDownLatch(10)
    def received = [].asSynchronized()

    when:
    harness.fork().start {
      rx.Observable.from((0..9).toList())
        .parallel { it.map { received << it; latch.countDown() } }
        .subscribe()
    }
    latch.await(2, TimeUnit.SECONDS)

    then:
    latch.count == 0
    received.sort() == (0..9).toList()
  }

  @Ignore("Waiting for release of RxJavaParallel")
  def "when using scheduler can use ratpack async api"() {
    given:
    def barrier = new CyclicBarrier(11)
    def received = [].asSynchronized()

    when:
    controller.control.fork {
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

  @Ignore("Waiting for release of RxJavaParallel")
  def "can use fork then join to perform parallel tasks"() {
    def latch = new CountDownLatch(1)
    List<Integer> nums = []

    when:
    harness.fork()
      .onComplete { latch.countDown() }
      .start { exec ->
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

      o.compose(RxRatpack.&bindExec).toList().subscribe {
        nums = it
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
    harness.run {
      sequence.forkEach().subscribe {
        received << it
        barrier.await()
      }
      barrier.await()
    }

    then:
    received.sort() == [1, 2, 3, 4, 5]
  }

  def "can add to registry of each fork"() {
    given:
    def sequence = rx.Observable.from(0, 1, 2, 3, 4)
    def barrier = new CyclicBarrier(6)
    def received = [].asSynchronized()
    Integer addMe = 1

    when:
    harness.run {

      sequence.forkEach({ RegistrySpec registrySpec -> registrySpec.add(addMe)}).subscribe {
        received << it + Execution.current().get(Integer)
        barrier.await()
      }
      barrier.await()
    }

    then:
    received.sort() == [1, 2, 3, 4, 5]
  }

  def "can use fork on next on observable"() {
    given:
    def sequence = rx.Observable.from("a", "b", "c", "d", "e")
    def barrier = new CyclicBarrier(6)
    def received = [].asSynchronized()

    when:
    harness.run {
      sequence.forkEach().subscribe {
        received << it.toUpperCase()
        barrier.await()
      }
      barrier.await()
    }

    then:
    received.sort() == ["A", "B", "C", "D", "E"]
  }

  def "errors are collected when using fork on next"() {
    given:
    def sequence = rx.Observable.from(1, 2, 3, 4, 5)
    def barrier = new CyclicBarrier(2)
    Throwable e = null

    when:
    harness.run {
      sequence.forkEach().serialize().subscribe({
        throw new RuntimeException("!")
      }, { e = it; barrier.await() })
      barrier.await()
    }

    then:
    e.message == "!"
  }

  def "can fork observable successfully to run on another compute thread"() {
    given:
    rx.Observable<Integer> sequence = rx.Observable.just(1)
    String harnessComputeThread = null
    String forkComputeThread = null

    when:
    Integer result = harness.yieldSingle {
      harnessComputeThread = Thread.currentThread().name

      sequence
        .doOnNext { forkComputeThread = Thread.currentThread().name }
        .fork()
        .promiseSingle()
    }.valueOrThrow

    then:
    harnessComputeThread != forkComputeThread && harnessComputeThread && forkComputeThread
    result == 1
  }

  def "all actions upstream from the fork run on the same compute thread, actions downstream are joined back to the original thread"() {
    given:
    rx.Observable<Integer> observable = rx.Observable.just(1)
    String harnessComputeThread = null
    String forkComputeThread = null

    when:
    Integer result = harness.yieldSingle {
      harnessComputeThread = Thread.currentThread().name

      observable
        .doOnNext { forkComputeThread = Thread.currentThread().name }
        .flatMap { rx.Observable.just(2) }
        .doOnNext { assert forkComputeThread == Thread.currentThread().name }
        .fork()
        .doOnNext { assert harnessComputeThread == Thread.currentThread().name }
        .promiseSingle()
    }.valueOrThrow

    then:
    harnessComputeThread != forkComputeThread && harnessComputeThread && forkComputeThread
    result == 2
  }

  def "multiple fork calls on an observable chain succeeds"() {
    given:
    String harnessComputeThread = null
    String firstForkedThread = null
    String secondForkedThread = null
    rx.Observable<Integer> observable = rx.Observable.just(1)

    when:
    Integer result = harness.yieldSingle {
      harnessComputeThread = Thread.currentThread().name

      observable
        .doOnNext { firstForkedThread = Thread.currentThread().name }
        .fork()
        .doOnNext { secondForkedThread = Thread.currentThread().name }
        .fork()
        .promiseSingle()

    }.valueOrThrow

    then:
    [ harnessComputeThread, firstForkedThread, secondForkedThread ].findAll { it }.unique().size() == 3
    result == 1
  }

  def "multiple forked observables run on separate threads with different types can be zipped together"() {
    given:
    String harnessComputeThread = null
    String firstForkedThread = null
    String secondForkedThread = null
    rx.Observable<Integer> first = rx.Observable.just(4).doOnNext { firstForkedThread = Thread.currentThread().name }
    rx.Observable<String> second = rx.Observable.just("5").doOnNext { secondForkedThread = Thread.currentThread().name }

    when:
    Integer result = harness.yieldSingle {
      harnessComputeThread = Thread.currentThread().name

      rx.Observable.zip(first.fork(), second.fork(), { Integer firstValue, String secondValue ->
        firstValue + secondValue.toInteger()
      }).promiseSingle()
    }.valueOrThrow

    then:
    [ harnessComputeThread, firstForkedThread, secondForkedThread ].findAll { it }.unique().size() == 3
    result == 9
  }

  def "multiple items emitted from the observable all run on the same forked thread"() {
    given:
    def sequence = rx.Observable.from([1, 2, 3])
    String harnessComputeThread = null
    String forkComputeThread = null

    when:
    Integer result = harness.yieldSingle {
      harnessComputeThread = Thread.currentThread().name

      sequence
        .doOnNext {
          if (it == 1) {
            forkComputeThread = Thread.currentThread().name
          }
          assert forkComputeThread == Thread.currentThread().name
        }
        .fork()
        .reduce(0) { acc, val -> acc + val }
        .promiseSingle()
    }.valueOrThrow

    then:
    harnessComputeThread != forkComputeThread && harnessComputeThread && forkComputeThread
    result == 6
  }

  def "an error on a forked observable is able to be seen on the original thread"() {
    given:
    def sequence = rx.Observable.from(1, 2, 3, 4, 5)
    Throwable e = null

    when:
    Integer result = harness.yieldSingle {
      sequence
        .doOnNext { if (it == 3) {
          throw new RuntimeException("3!") }
        }
        .fork()
        .doOnError { Throwable t -> e = t }
        .onErrorReturn({ -1 })
        .promiseSingle()
    }.valueOrThrow

    then:
    e.message == "3!"
    result == -1

  }

  def "can make objects available to the forked execution's registry"() {
    given:
    rx.Observable<Integer> sequence = rx.Observable.just(1)
    String harnessComputeThread = null
    String forkComputeThread = null
    String forkedValue = null

    when:
    Integer result = harness.yieldSingle {
      Execution.current().add("foo")
      harnessComputeThread = Thread.currentThread().name
      String originalValue = Execution.current().get(String)

      sequence
        .doOnNext { forkComputeThread = Thread.currentThread().name }
        .doOnNext { forkedValue = Execution.current().get(String) }
        .fork { RegistrySpec registrySpec -> registrySpec.add(originalValue + "bar") }
        .promiseSingle()
    }.valueOrThrow

    then:
    "foobar" == forkedValue
    harnessComputeThread != forkComputeThread && harnessComputeThread && forkComputeThread
    result == 1
  }

}
