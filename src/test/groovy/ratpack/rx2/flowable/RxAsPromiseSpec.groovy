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
