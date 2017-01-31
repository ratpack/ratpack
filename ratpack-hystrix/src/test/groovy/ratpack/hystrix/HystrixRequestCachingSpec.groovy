/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.hystrix

import com.google.inject.Inject
import com.netflix.hystrix.HystrixCommand
import com.netflix.hystrix.HystrixCommandGroupKey
import com.netflix.hystrix.HystrixObservableCommand
import ratpack.error.ServerErrorHandler
import ratpack.exec.Blocking
import ratpack.handling.Context
import ratpack.http.client.BaseHttpClientSpec
import ratpack.http.client.HttpClient
import ratpack.http.client.ReceivedResponse
import ratpack.rx.RxRatpack
import rx.Observable

@SuppressWarnings("GrMethodMayBeStatic")
class HystrixRequestCachingSpec extends BaseHttpClientSpec {

  def setup() {
    RxRatpack.initialize()
  }

  def "can handle error from hystrix command"() {
    when:
    bindings {
      module new HystrixModule()
      bindInstance ServerErrorHandler, new ServerErrorHandler() {
        @Override
        void error(Context context, Throwable throwable) throws Exception {
          context.render "exception"
        }
      }
    }

    handlers {
      get {
        new HystrixObservableCommand<String>(HystrixCommandGroupKey.Factory.asKey("test-dummy-key")) {
          @Override
          protected Observable<String> construct() {
            throw new Exception("Exception from hystrix run command")
          }
        }.toObservable()
          .subscribe {
          render "Subscribe success - $it"
        }
      }
    }

    then:
    text == "exception"
  }

  def "can cache #executionType command execution"() {
    given:
    otherApp {
      get("foo/:id") { render pathTokens.get("id") }
    }

    and:
    bindings {
      module new HystrixModule()
      bind(CommandFactory)
    }

    handlers { CommandFactory factory ->
      path("blocking") {
        def firstCall = factory.hystrixCommand("1").queue()
        Blocking.get {
          firstCall.get()
        } then {
          def secondCall = factory.hystrixCommand("2").queue()
          Blocking.get {
            secondCall.get()
          } then {
            render it
          }
        }
      }

      path("observable") {
        factory.hystrixObservableCommand("1").flatMap {
          factory.hystrixObservableCommand("2")
        } subscribe {
          render it
        }
      }

      path("blocking-observable") {
        factory.hystrixBlockingObservableCommand("1").flatMap {
          factory.hystrixBlockingObservableCommand("2")
        } subscribe {
          render it
        }
      }

      path("http-observable") {
        factory.hystrixObservableHttpCommand(otherAppUrl("foo/1")).flatMap {
          factory.hystrixObservableHttpCommand(otherAppUrl("foo/2")).map { ReceivedResponse resp ->
            return resp.body.text
          }
        } subscribe {
          render it
        }
      }
    }

    expect:
    getText(executionType) == "1"

    where:
    executionType << ["blocking", "observable", "blocking-observable", "http-observable"]
  }

  static class CommandFactory {

    private final HttpClient httpClient

    @Inject
    CommandFactory(HttpClient httpClient) {
      this.httpClient = httpClient
    }

    HystrixCommand<String> hystrixCommand(final String requestNumber) {
      new HystrixCommand<String>(HystrixCommandGroupKey.Factory.asKey("hystrix-command")) {

        @Override
        protected String run() throws Exception {
          assert Thread.currentThread().name.startsWith("hystrix-${getCacheKey()}-")
          requestNumber
        }

        @Override
        protected String getCacheKey() {
          return "hystrix-command"
        }
      }
    }

    rx.Observable<String> hystrixObservableCommand(final String requestNumber) {
      new HystrixObservableCommand<String>(HystrixCommandGroupKey.Factory.asKey("hystrix-observable-command")) {

        @Override
        protected String getCacheKey() {
          return "hystrix-observable-command"
        }

        @Override
        protected Observable<String> construct() {
          assert Thread.currentThread().name.startsWith("ratpack-compute-")
          return rx.Observable.just(requestNumber)
        }
      }.toObservable()
    }

    rx.Observable<String> hystrixBlockingObservableCommand(final String requestNumber) {
      new HystrixObservableCommand<String>(HystrixCommandGroupKey.Factory.asKey("hystrix-blocking-observable-command")) {

        @Override
        protected String getCacheKey() {
          return "hystrix-blocking-observable-command"
        }

        @Override
        protected Observable<String> construct() {
          assert Thread.currentThread().name.startsWith("ratpack-compute-")
          return RxRatpack.observe(Blocking.get {
            assert Thread.currentThread().name.startsWith("ratpack-blocking-")
            return requestNumber
          })
        }
      }.toObservable()
    }

    rx.Observable<ReceivedResponse> hystrixObservableHttpCommand(final URI otherAppUrl) {
      new HystrixObservableCommand<String>(HystrixCommandGroupKey.Factory.asKey("hystrix-observable-http-command")) {

        @Override
        protected String getCacheKey() {
          return "hystrix-http-observable-command"
        }

        @Override
        protected Observable<String> construct() {
          assert Thread.currentThread().name.startsWith("ratpack-compute-")
          return RxRatpack.observe(httpClient.get(otherAppUrl) {})
        }
      }.toObservable()
    }
  }
}
