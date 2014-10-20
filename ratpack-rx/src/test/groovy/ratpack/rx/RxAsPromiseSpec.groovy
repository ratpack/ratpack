package ratpack.rx

import ratpack.exec.ExecControl
import ratpack.test.exec.ExecHarness
import rx.Observable
import spock.lang.AutoCleanup
import spock.lang.Specification

import static ratpack.rx.RxRatpack.asPromise
import static ratpack.rx.RxRatpack.asPromiseSingle

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
    def result = harness.yield { asPromise(service.observe("foo")) }

    then:
    result.valueOrThrow == ["foo"]
  }

  def "failed observable causes exception to be thrown"() {
    when:
    harness.yield { asPromise(service.fail()) }.valueOrThrow

    then:
    def e = thrown RuntimeException
    e.message == "!!!"
  }

  def "can unpack single"() {
    when:
    def result = harness.yield { asPromiseSingle(service.observe("foo")) }

    then:
    result.valueOrThrow == "foo"
  }

}
