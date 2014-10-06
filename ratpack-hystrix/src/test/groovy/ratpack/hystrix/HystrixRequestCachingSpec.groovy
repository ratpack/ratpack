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
import ratpack.exec.ExecController
import ratpack.handling.Context
import ratpack.http.client.HttpClient
import ratpack.http.client.HttpClientSpec
import ratpack.http.client.ReceivedResponse
import ratpack.rx.RxRatpack
import spock.lang.Unroll

@SuppressWarnings("GrMethodMayBeStatic")
class HystrixRequestCachingSpec extends HttpClientSpec {

  def setup() {
    RxRatpack.initialize()
  }

  def "can handle error from hystrix command"() {
    when:
    bindings {
      add new HystrixModule()
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
          protected rx.Observable<String> run() {
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

  @Unroll
  def "can cache #executionType command execution"() {
    given:
    otherApp {
      get("foo/:id") { render pathTokens.get("id") }
    }

    and:
    bindings {
      add new HystrixModule()
      bind(CommandFactory)
    }

    handlers { CommandFactory factory ->
      handler("blocking") {
        def firstCall = factory.hystrixCommand("1").queue()
        blocking {
          firstCall.get()
        } then {
          def secondCall = factory.hystrixCommand("2").queue()
          blocking {
            secondCall.get()
          } then {
            render it
          }
        }
      }

      handler("observable") {
        factory.hystrixObservableCommand("1").flatMap {
          factory.hystrixObservableCommand("2")
        } subscribe {
          render it
        }
      }

      handler("blocking-observable") {
        factory.hystrixBlockingObservableCommand("1").flatMap {
          factory.hystrixBlockingObservableCommand("2")
        } subscribe {
          render it
        }
      }

      handler("http-observable") {
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
        protected rx.Observable<String> run() {
          assert Thread.currentThread().name.startsWith("ratpack-compute-")
          return rx.Observable.just(requestNumber)
        }

        @Override
        protected String getCacheKey() {
          return "hystrix-observable-command"
        }
      }.toObservable()
    }

    rx.Observable<String> hystrixBlockingObservableCommand(final String requestNumber) {
      new HystrixObservableCommand<String>(HystrixCommandGroupKey.Factory.asKey("hystrix-blocking-observable-command")) {

        @Override
        protected rx.Observable<String> run() {
          assert Thread.currentThread().name.startsWith("ratpack-compute-")
          return RxRatpack.observe(ExecController.current().get().control.blocking {
            assert Thread.currentThread().name.startsWith("ratpack-blocking-")
            return requestNumber
          })
        }

        @Override
        protected String getCacheKey() {
          return "hystrix-blocking-observable-command"
        }
      }.toObservable()
    }

    rx.Observable<ReceivedResponse> hystrixObservableHttpCommand(final URI otherAppUrl) {
      new HystrixObservableCommand<String>(HystrixCommandGroupKey.Factory.asKey("hystrix-observable-http-command")) {

        @Override
        protected rx.Observable<ReceivedResponse> run() {
          assert Thread.currentThread().name.startsWith("ratpack-compute-")
          return RxRatpack.observe(httpClient.get(otherAppUrl) {})
        }

        @Override
        protected String getCacheKey() {
          return "hystrix-http-observable-command"
        }
      }.toObservable()
    }
  }
}
