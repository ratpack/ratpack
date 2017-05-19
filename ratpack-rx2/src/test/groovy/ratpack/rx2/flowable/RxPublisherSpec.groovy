package ratpack.rx2.flowable

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import ratpack.exec.Blocking
import ratpack.rx2.RxRatpack
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification

class RxPublisherSpec extends Specification {

  @AutoCleanup
  ExecHarness harness = ExecHarness.harness()
  AsyncService service = new AsyncService()

  static class AsyncService {
    public Flowable<Void> fail() {
      RxRatpack.flow(Blocking.get { throw new RuntimeException("!!!") }, BackpressureStrategy.BUFFER)
    }

    public <T> Flowable<T> flow(T value) {
      RxRatpack.flow(Blocking.get { value }, BackpressureStrategy.BUFFER)
    }
  }

  def setup() {
    RxRatpack.initialize()
  }

  def "convert RX Flowable to ReactiveStreams Publisher"() {
    given:
    Publisher<String> pub = service.flow("foo").publisher()
    def received = []
    Subscription subscription
    boolean complete = false

    when:
    harness.run {
      pub.subscribe(new Subscriber<String>() {
        @Override
        void onSubscribe(Subscription s) {
          subscription = s
        }

        @Override
        void onNext(String s) {
          received << s
        }

        @Override
        void onError(Throwable t) {
          received << t
        }

        @Override
        void onComplete() {
          complete = true
        }
      })
      subscription.request(1)
    }

    then:
    received.size() == 1
    received.first() == 'foo'
    complete
  }

}
