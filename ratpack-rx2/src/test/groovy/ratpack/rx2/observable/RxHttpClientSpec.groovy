package ratpack.rx2.observable

import io.reactivex.BackpressureStrategy
import io.reactivex.functions.Function
import ratpack.http.client.HttpClient
import ratpack.rx2.RxRatpack
import ratpack.http.client.BaseHttpClientSpec

import static ratpack.rx2.RxRatpack.flow

class RxHttpClientSpec extends BaseHttpClientSpec {

  def setup() {
    RxRatpack.initialize()
  }

  def "can use rx with http client"() {
    given:
    otherApp {
      get("foo") { render "bar" }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        flow(httpClient.get(otherAppUrl("foo")) {}, BackpressureStrategy.BUFFER) map({
          it.body.text.toUpperCase()
        } as Function) subscribe {
          render it
        }
      }
    }

    then:
    text == "BAR"
  }

}
