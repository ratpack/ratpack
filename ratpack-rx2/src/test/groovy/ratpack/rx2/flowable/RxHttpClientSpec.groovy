package ratpack.rx2.flowable

import ratpack.http.client.HttpClient
import ratpack.rx2.RxRatpack
import ratpack.rx2.internal.BaseHttpClientSpec

import static ratpack.rx2.RxRatpack.observe

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
        observe(httpClient.get(otherAppUrl("foo")) {}) map {
          it.body.text.toUpperCase()
        } subscribe {
          render it
        }
      }
    }

    then:
    text == "BAR"
  }

}
