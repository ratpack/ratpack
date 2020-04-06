package ratpack.micrometer.metrics.internal

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import ratpack.http.client.BaseHttpClientSpec
import ratpack.http.client.HttpClient
import ratpack.micrometer.metrics.MicrometerMetricsModule
import spock.lang.Timeout

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import static ratpack.micrometer.metrics.ClientRequestTags.*

class HttpClientTimingActionSpec extends BaseHttpClientSpec {

  @Timeout(5)
  def 'can time client requests'() {
    given:
    CountDownLatch latch = new CountDownLatch(1)
    MeterRegistry meterRegistry = new SimpleMeterRegistry()

    otherApp {
      get {
        render 'foo'
      }
    }

    bindings {
//      bindInstance HttpClient, HttpClient.of(
//          new HttpClientTimingAction(meterRegistry, { request, response ->
//            Tags.of(
//              method(request),
//              status(response),
//              outcome(response)
//            )
//          }).append({ spec ->
//            spec.requestIntercept { request ->
//              request.headers { headers ->
//                headers.add 'X-GLOBAL', 'foo'
//              }
//            }
//            spec.responseIntercept { response ->
//              latch.countDown()
//            }
//          }
//        )
//      )

      module new MicrometerMetricsModule(), {
        it.additionalMeterRegistries(meterRegistry)
        it.clientRequestTags { request, response ->
          Tags.of(
            method(request),
            status(response),
            outcome(response)
          )
        }
      }
    }

    handlers {
      get { HttpClient client ->
        render client.get(otherAppUrl("")).map { it.body.text }
      }
    }

    when:
    def result = text
    latch.await(5, TimeUnit.SECONDS)

    then:
    result == 'foo'

    meterRegistry?.get('http.client.requests')
      ?.tag('method', 'GET')
      ?.tag('status', '200')
      ?.tag('outcome', 'SUCCESSFUL')
      ?.timer()?.count() == 1
  }
}
