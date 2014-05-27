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

import com.netflix.hystrix.HystrixCommand
import com.netflix.hystrix.HystrixCommandGroupKey
import com.netflix.hystrix.HystrixObservableCommand
import ratpack.exec.internal.DefaultExecController
import ratpack.http.client.HttpClients
import ratpack.http.client.ReceivedResponse

import static ratpack.rx.RxRatpack.observe

abstract class CommandFactory {

  static HystrixCommand<String> hystrixCommand(final String requestNumber) {
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

  static rx.Observable<String> hystrixObservableCommand(final String requestNumber) {
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

  static rx.Observable<String> hystrixBlockingObservableCommand(final String requestNumber) {
    new HystrixObservableCommand<String>(HystrixCommandGroupKey.Factory.asKey("hystrix-blocking-observable-command")) {

      @Override
      protected rx.Observable<String> run() {
        assert Thread.currentThread().name.startsWith("ratpack-compute-")
        return observe(DefaultExecController.threadBoundController.get().control.blocking {
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

  static rx.Observable<ReceivedResponse> hystrixObservableHttpCommand(final String otherAppUrl) {
    new HystrixObservableCommand<String>(HystrixCommandGroupKey.Factory.asKey("hystrix-observable-http-command")) {

      @Override
      protected rx.Observable<ReceivedResponse> run() {
        assert Thread.currentThread().name.startsWith("ratpack-compute-")
        return observe(HttpClients.httpClient(DefaultExecController.threadBoundContext.launchConfig).get(otherAppUrl))
      }

      @Override
      protected String getCacheKey() {
        return "hystrix-http-observable-command"
      }
    }.toObservable()
  }
}
