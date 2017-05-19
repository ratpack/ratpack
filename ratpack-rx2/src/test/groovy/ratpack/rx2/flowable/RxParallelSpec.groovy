package ratpack.rx2.flowable

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function
import ratpack.exec.Execution
import ratpack.registry.RegistrySpec
import ratpack.rx2.RxRatpack
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.concurrent.CyclicBarrier

class RxParallelSpec extends Specification {

  @AutoCleanup
  def harness = ExecHarness.harness(12)

  def setup() {
    RxRatpack.initialize()
  }

  def "can use fork on next to fan out"() {
    given:
    def sequence = Flowable.fromArray(1, 2, 3, 4, 5)
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
    def sequence = Flowable.fromArray(0, 1, 2, 3, 4)
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
    def sequence = Flowable.fromArray("a", "b", "c", "d", "e")
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
    def sequence = Flowable.fromArray(1, 2, 3, 4, 5)
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
    Flowable<Integer> sequence = Flowable.just(1)
    String harnessComputeThread = null
    String forkComputeThread = null

    when:
    Integer result = harness.yieldSingle {
      harnessComputeThread = Thread.currentThread().name

      sequence
        .doOnNext { forkComputeThread = Thread.currentThread().name }
        .fork(BackpressureStrategy.BUFFER)
        .promiseSingle()
    }.valueOrThrow

    then:
    harnessComputeThread != forkComputeThread && harnessComputeThread && forkComputeThread
    result == 1
  }

  def "all actions upstream from the fork run on the same compute thread, actions downstream are joined back to the original thread"() {
    given:
    Flowable<Integer> flowable = Flowable.just(1)
    String harnessComputeThread = null
    String forkComputeThread = null

    when:
    Integer result = harness.yieldSingle {
      harnessComputeThread = Thread.currentThread().name

      flowable
        .doOnNext { forkComputeThread = Thread.currentThread().name }
        .flatMap({ Flowable.just(2) } as Function)
        .doOnNext { assert forkComputeThread == Thread.currentThread().name }
        .fork(BackpressureStrategy.BUFFER)
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
    Flowable<Integer> flowable = Flowable.just(1)

    when:
    Integer result = harness.yieldSingle {
      harnessComputeThread = Thread.currentThread().name

      flowable
        .doOnNext { firstForkedThread = Thread.currentThread().name }
        .fork(BackpressureStrategy.BUFFER)
        .doOnNext { secondForkedThread = Thread.currentThread().name }
        .fork(BackpressureStrategy.BUFFER)
        .promiseSingle()

    }.valueOrThrow

    then:
    [harnessComputeThread, firstForkedThread, secondForkedThread].findAll { it }.unique().size() == 3
    result == 1
  }

  def "multiple forked observables run on separate threads with different types can be zipped together"() {
    given:
    String harnessComputeThread = null
    String firstForkedThread = null
    String secondForkedThread = null
    Flowable<Integer> first = Flowable.just(4).doOnNext { firstForkedThread = Thread.currentThread().name }
    Flowable<String> second = Flowable.just("5").doOnNext { secondForkedThread = Thread.currentThread().name }

    when:
    Integer result = harness.yieldSingle {
      harnessComputeThread = Thread.currentThread().name

      Flowable.zip(first.fork(BackpressureStrategy.BUFFER), second.fork(BackpressureStrategy.BUFFER), { Integer firstValue, String secondValue ->
        firstValue + secondValue.toInteger()
      }).promiseSingle()
    }.valueOrThrow

    then:
    [harnessComputeThread, firstForkedThread, secondForkedThread].findAll { it }.unique().size() == 3
    result == 9
  }

  def "multiple items emitted from the observable all run on the same forked thread"() {
    given:
    def sequence = Flowable.fromIterable([1, 2, 3])
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
      .fork(BackpressureStrategy.BUFFER)
        .reduce(0, { acc, val -> acc + val } as BiFunction).promiseSingle()
    }.value

    then:
    harnessComputeThread != forkComputeThread && harnessComputeThread && forkComputeThread
    result == 6
  }

  def "an error on a forked observable is able to be seen on the original thread"() {
    given:
    def sequence = Flowable.fromArray(1, 2, 3, 4, 5)
    Throwable e = null

    when:
    Integer result = harness.yieldSingle {
      sequence
        .doOnNext {
        if (it == 3) {
          throw new RuntimeException("3!")
        }
      }
      .fork(BackpressureStrategy.BUFFER)
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
    Flowable<Integer> sequence = Flowable.just(1)
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
        .fork(BackpressureStrategy.BUFFER) { RegistrySpec registrySpec -> registrySpec.add(originalValue + "bar") }
        .promiseSingle()
    }.valueOrThrow

    then:
    "foobar" == forkedValue
    harnessComputeThread != forkComputeThread && harnessComputeThread && forkComputeThread
    result == 1
  }

}
