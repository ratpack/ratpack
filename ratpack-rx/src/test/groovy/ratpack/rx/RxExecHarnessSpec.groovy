package ratpack.rx

import ratpack.test.exec.ExecHarness
import rx.Observable
import spock.lang.Specification

import static ratpack.rx.RxRatpack.promise

class RxExecHarnessSpec extends Specification {

  private AsyncService service = new AsyncService()

  static class AsyncService {

    public Observable<Void> fail() {
      Observable.error(new RuntimeException("!!!"))
    }

    public <T> Observable<T> observe(T value) {
      Observable.just(value)
    }
  }

  def "can test async service"() {
    when:
    def result = ExecHarness.yieldSingle {
      promise(service.observe("foo"))
    }

    then:
    result.value == ["foo"]
  }

  def "failed observable causes exception to be thrown"() {
    when:
    ExecHarness.yieldSingle {
      promise(service.fail())
    }.valueOrThrow

    then:
    def e = thrown RuntimeException
    e.message == "!!!"
  }

}
