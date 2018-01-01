package ratpack.rx2.observable

import io.reactivex.Observable
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

    public Observable<Void> fail() {
      RxRatpack.observe(Blocking.get { throw new RuntimeException("!!!") })
    }

    public <T> Observable<T> observe(T value) {
      RxRatpack.observe(Blocking.get { value })
    }

    public Observable<Void> increment() {
      RxRatpack.observe(Operation.of { counter++ })
    }
  }

  def setup() {
    RxRatpack.initialize()
  }

  def "can test async service"() {
    when:
    def result = harness.yield { service.observe("foo").promise() }

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
    def result = harness.yield { service.observe("foo").promiseSingle() }

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
