/*
 * Copyright 2018 the original author or authors.
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

package ratpack.reactor.flux

import org.reactivestreams.Subscription
import ratpack.error.ServerErrorHandler
import ratpack.exec.ExecController
import ratpack.exec.Promise
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.groovy.test.handling.GroovyRequestFixture
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.reactor.ReactorRatpack
import ratpack.test.internal.RatpackGroovyDslSpec
import reactor.core.CoreSubscriber
import reactor.core.Exceptions
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers

import java.util.concurrent.Executors
import java.util.function.Function

class ReactorErrorHandlingSpec extends RatpackGroovyDslSpec {

  static class MessagePrintingErrorHandler implements ServerErrorHandler {
    List<Throwable> errors = []

    @Override
    void error(Context context, Throwable throwable) throws Exception {
      errors << throwable
      context.render("threw throwable")
    }
  }

  def errorHandler = new MessagePrintingErrorHandler()
  def error = new RuntimeException("!")

  def setup() {
    ReactorRatpack.initialize()
    bindings {
      bindInstance ServerErrorHandler, errorHandler
    }
  }

  Throwable getThrownException() {
    assert text == "threw throwable"
    errorHandler.errors.last()
  }

  def "no error handler for failed promise"() {
    when:
    handlers {
      get {
        Promise.error(error).flux().subscribe {
          render "got to end"
        }
      }
    }

    then:
    thrownException.cause == error
  }

  def "observable sequence without error handler fulfills with Error subclass"() {
    when:
    def e = new Error("Error")
    handlers {
      get {
        Promise.error(e).flux().subscribe {
          render "got to end"
        }
      }
    }

    then:
    thrownException.cause == e
  }

  def "no error handler for successful promise that throws"() {
    when:
    handlers {
      get {
        Promise.value("").flux().subscribe {
          throw error
        }
      }
    }

    then:
    thrownException.cause == error
  }

  def "no error handler for successful promise that has nested failing promise"() {
    when:
    handlers {
      get {
        Promise.value("").flux().subscribe {
          Promise.error(error).flux().subscribe {
            render "got to end"
          }
        }
      }
    }

    then:
    thrownException.cause == error
  }

  def "no error handler for successful promise that has nested successful promise that throws"() {
    when:
    handlers {
      get {
        Promise.value("").flux().subscribe {
          Promise.value("").flux().subscribe {
            throw error
          }
        }
      }
    }

    then:
    thrownException.cause == error
  }

  def "error handler receives promise error"() {
    when:
    def otherError = new RuntimeException("other")
    handlers {
      get {
        Promise.error(error).flux().subscribe {
          render "success"
        } {
          throw otherError
        }
      }
    }

    then:
    def t = thrownException
    t instanceof RuntimeException
  }

  def "subscription without error handler results in error forwarded to context error handler"() {
    given:
    def e = new Exception("!")

    when:
    handlers {
      get {
        Flux.error(e).subscribe {} {}
      }
    }

    then:
    get()
    thrownException == e
  }

  def "observer can be complex"() {
    given:
    def e = new Exception("!")

    when:
    handlers {
      get {
        Flux.error(e).map({ 2 } as Function).subscribe()
      }
    }

    then:
    get()
    thrownException.cause == e
  }

  def "subscriber can throw"() {
    given:
    def e = new Exception("!")

    when:
    handlers {
      get {
        Flux.just("foo").subscribe {
          throw e
        }
      }
    }

    then:
    get()
    thrownException.cause.cause == e
  }

  def "downstream handler can error"() {
    given:
    def e = new Exception("!")

    when:
    handlers {
      get {
        Flux.just("foo").subscribe {
          next()
        }
      }
      all {
        throw e
      }
    }

    then:
    get()
    thrownException.cause.cause == e
  }

  def "can use two different rx ratpack apps in same jvm"() {
    when:
    def app1 = GroovyEmbeddedApp.of {
      registryOf {
        add ServerErrorHandler, new ServerErrorHandler() {
          @Override
          void error(Context context, Throwable throwable) throws Exception {
            context.render "app1"
          }
        }
      }
      handlers {
        get { Flux.error(new Exception("1")).subscribe() }
      }
    }
    def app2 = GroovyEmbeddedApp.of {
      registryOf {
        add ServerErrorHandler, new ServerErrorHandler() {
          @Override
          void error(Context context, Throwable throwable) throws Exception {
            context.render "app2"
          }
        }
      }
      handlers {
        get { Flux.error(new Exception("2")).subscribe() }
      }
    }

    then:
    app1.httpClient.text == "app1"
    app2.httpClient.text == "app2"
  }

  def "can use rx in a unit test"() {
    given:
    def e = new Exception("!")

    when:
    def result = GroovyRequestFixture.handle({ Flux.error(e).subscribe({}, {}) } as Handler) {
    }

    then:
    result.exception(Exception) == e
  }

  def "error handler is invoked even when error occurs on different thread"() {
    given:
    handlers {
      get { ExecController execController ->
        Promise.async { f ->
          execController.executor.execute {
            Flux.error(error).subscribe(new CoreSubscriber<Object>() {
              @Override
              void onSubscribe(Subscription s) {

              }

              @Override
              void onComplete() {

              }

              @Override
              void onError(Throwable e) {
                f.error(e)
              }

              @Override
              void onNext(Object o) {

              }
            })
          }
        } then {
          render "unexpected"
        }
      }
    }


    when:
    get()

    then:
    errorHandler.errors == [error]
  }

  def "composed observable errors"() {
    given:
    def e = new Exception("!")

    when:
    handlers {
      get {
        Flux.just(1).
          flatMap({ Flux.error(e) } as Function).
          subscribe { render "onNext" }
      }
    }

    then:
    thrownException.cause == e
  }

  def "exception thrown by oncomplete is propagated"() {
    when:
    handlers {
      get {
        Flux.just(1).
          subscribe(new CoreSubscriber<Integer>() {
            @Override
            void onSubscribe(Subscription s) {
              s.request(1)
            }

            @Override
            void onComplete() {
              throw error
            }

            @Override
            void onError(Throwable t) {
              throw Exceptions.errorCallbackNotImplemented(t)
            }

            @Override
            void onNext(Integer integer) {
            }
          })
      }
    }

    then:
    get()
    def e = errorHandler.errors.first()
    e == error
  }

  def "exception thrown by onerror are propagated"() {
    when:
    handlers {
      get {
        Flux.error(new RuntimeException("1")).
          subscribe(new CoreSubscriber<Integer>() {
            @Override
            void onSubscribe(Subscription s) {
            }

            @Override
            void onComplete() {
              throw new UnsupportedOperationException()
            }

            @Override
            void onError(Throwable t) {
              throw error
            }

            @Override
            void onNext(Integer integer) {
            }
          })
      }
    }

    then:
    get()
    def e = errorHandler.errors.first()
    e == error
  }

  def "exceptions from bound observable are propagated"() {
    when:
    handlers {
      get {
        Flux.error(error).subscribeOn(Schedulers.fromExecutor(Executors.newSingleThreadExecutor())).bindExec().promise()
          .onError { render it.toString() }
          .then { render "no error" }
      }
    }

    then:
    text == error.toString()
  }
}
