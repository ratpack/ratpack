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

import ratpack.exec.Execution
import ratpack.reactor.ReactorRatpack
import ratpack.registry.RegistrySpec
import ratpack.test.exec.ExecHarness
import reactor.core.publisher.Flux
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.concurrent.CyclicBarrier
import java.util.function.BiFunction
import java.util.function.Function

class ReactorParallelSpec extends Specification {

  @AutoCleanup
  def harness = ExecHarness.harness(12)

  def setup() {
    ReactorRatpack.initialize()
  }

  def "can use fork on next to fan out"() {
    given:
    def sequence = Flux.fromArray(1, 2, 3, 4, 5)
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
    def sequence = Flux.fromArray(0, 1, 2, 3, 4)
    def barrier = new CyclicBarrier(6)
    def received = [].asSynchronized()
    Integer addMe = 1

    when:
    harness.run {

      sequence.forkEach({ RegistrySpec registrySpec -> registrySpec.add(addMe) }).subscribe {
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
    def sequence = Flux.fromArray("a", "b", "c", "d", "e")
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
    def sequence = Flux.fromArray(1, 2, 3, 4, 5)
    def barrier = new CyclicBarrier(2)
    Throwable e = null

    when:
    harness.run {
      sequence.forkEach().subscribe({
        throw new RuntimeException("!")
      }, { e = it; barrier.await() })
      barrier.await()
    }

    then:
    e.message == "!"
  }

  def "can fork observable successfully to run on another compute thread"() {
    given:
    Flux<Integer> sequence = Flux.just(1)
    String harnessComputeThread = null
    String forkComputeThread = null

    when:
    List<Integer> result = harness.yieldSingle {
      harnessComputeThread = Thread.currentThread().name

      sequence
        .doOnNext { forkComputeThread = Thread.currentThread().name }
        .fork()
        .promise()
    }.valueOrThrow

    then:
    harnessComputeThread != forkComputeThread && harnessComputeThread && forkComputeThread
    result == [1]
  }

  def "all actions upstream from the fork run on the same compute thread, actions downstream are joined back to the original thread"() {
    given:
    Flux<Integer> flowable = Flux.just(1)
    String harnessComputeThread = null
    String forkComputeThread = null

    when:
    List<Integer> result = harness.yieldSingle {
      harnessComputeThread = Thread.currentThread().name

      flowable
        .doOnNext { forkComputeThread = Thread.currentThread().name }
        .flatMap({ Flux.just(1,2) } as Function)
        .doOnNext { assert forkComputeThread == Thread.currentThread().name }
        .fork()
        .doOnNext { assert harnessComputeThread == Thread.currentThread().name }
        .promise()
    }.valueOrThrow

    then:
    harnessComputeThread != forkComputeThread && harnessComputeThread && forkComputeThread
    result == [1,2]
  }

  def "multiple fork calls on an observable chain succeeds"() {
    given:
    String harnessComputeThread = null
    String firstForkedThread = null
    String secondForkedThread = null
    Flux<Integer> flowable = Flux.just(1,2)

    when:
    List<Integer> result = harness.yieldSingle {
      harnessComputeThread = Thread.currentThread().name

      flowable
        .doOnNext { firstForkedThread = Thread.currentThread().name }
        .fork()
        .doOnNext { secondForkedThread = Thread.currentThread().name }
        .fork()
        .promise()

    }.valueOrThrow

    then:
    [harnessComputeThread, firstForkedThread, secondForkedThread].findAll { it }.unique().size() == 3
    result == [1,2]
  }

  def "multiple forked observables run on separate threads with different types can be zipped together"() {
    given:
    String harnessComputeThread = null
    String firstForkedThread = null
    String secondForkedThread = null
    Flux<Integer> first = Flux.just(4).doOnNext { firstForkedThread = Thread.currentThread().name }
    Flux<String> second = Flux.just("5").doOnNext { secondForkedThread = Thread.currentThread().name }

    when:
    List<Integer> result = harness.yieldSingle {
      harnessComputeThread = Thread.currentThread().name

      Flux.zip(first.fork(), second.fork(), { Integer firstValue, String secondValue ->
        firstValue + secondValue.toInteger()
      } as BiFunction<Integer, String, Integer>).promise()
    }.valueOrThrow

    then:
    [harnessComputeThread, firstForkedThread, secondForkedThread].findAll { it }.unique().size() == 3
    result == [9]
  }

  def "multiple items emitted from the observable all run on the same forked thread"() {
    given:
    def sequence = Flux.fromIterable([1, 2, 3])
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
      .fork().reduce(0, { acc, val -> acc + val } as BiFunction<Integer, Integer, Integer>).promiseSingle()
    }.value

    then:
    harnessComputeThread != forkComputeThread && harnessComputeThread && forkComputeThread
    result == 6
  }

  def "an error on a forked observable is able to be seen on the original thread"() {
    given:
    def sequence = Flux.fromArray(1, 2, 3, 4, 5)
    Throwable e = null

    when:
    List<Integer> result = harness.yieldSingle {
      sequence
        .doOnNext {
        if (it == 3) {
          throw new RuntimeException("3!")
        }
      }
      .fork()
        .doOnError { Throwable t -> e = t }
        .onErrorReturn(-1)
        .promise()
    }.valueOrThrow

    then:
    e.message == "3!"
    result == [-1]

  }

  def "can make objects available to the forked execution's registry"() {
    given:
    Flux<Integer> sequence = Flux.just(1)
    String harnessComputeThread = null
    String forkComputeThread = null
    String forkedValue = null

    when:
    List<Integer> result = harness.yieldSingle {
      Execution.current().add("foo")
      harnessComputeThread = Thread.currentThread().name
      String originalValue = Execution.current().get(String)

      sequence
        .doOnNext { forkComputeThread = Thread.currentThread().name }
        .doOnNext { forkedValue = Execution.current().get(String) }
        .fork { RegistrySpec registrySpec -> registrySpec.add(originalValue + "bar") }
        .promise()
    }.valueOrThrow

    then:
    "foobar" == forkedValue
    harnessComputeThread != forkComputeThread && harnessComputeThread && forkComputeThread
    result == [1]
  }

}
