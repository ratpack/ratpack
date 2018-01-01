package ratpack.rx2.single

import io.reactivex.Single
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

    public Single<Void> fail() {
      RxRatpack.single(Blocking.get { throw new RuntimeException("!!!") })
    }

    public <T> Single<T> single(T value) {
      RxRatpack.single(Blocking.get { value })
    }
  }

  def setup() {
    RxRatpack.initialize()
  }

  def "can test async service"() {
    when:
    def result = harness.yield { service.single("foo").promiseSingle() }

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

  def "can unpack single"() {
    when:
    def result = harness.yield { service.single("foo").promiseSingle() }

    then:
    result.valueOrThrow == "foo"
  }

}
