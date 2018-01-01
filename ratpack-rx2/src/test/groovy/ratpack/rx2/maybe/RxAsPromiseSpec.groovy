package ratpack.rx2.maybe

import io.reactivex.Maybe
import ratpack.exec.Blocking
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

    public Maybe<Void> fail() {
      RxRatpack.maybe(Blocking.get { throw new RuntimeException("!!!") })
    }

    public <T> Maybe<T> maybe(T value) {
      RxRatpack.maybe(Blocking.get { value })
    }
  }

  def setup() {
    RxRatpack.initialize()
  }

  def "can test async service"() {
    when:
    def result = harness.yield { service.maybe("foo").promiseSingle() }

    then:
    result.valueOrThrow == "foo"
  }

  def "failed observable causes exception to be thrown"() {
    when:
    harness.yield { service.fail().promiseSingle() }.valueOrThrow

    then:
    def e = thrown RuntimeException
    e.message == "!!!"
  }

  def "can unpack maybe"() {
    when:
    def result = harness.yield { service.maybe("foo").promiseSingle() }

    then:
    result.valueOrThrow == "foo"
  }

}
