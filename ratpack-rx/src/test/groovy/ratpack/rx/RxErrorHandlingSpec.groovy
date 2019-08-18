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

package ratpack.rx

import ratpack.error.ServerErrorHandler
import ratpack.exec.ExecController
import ratpack.exec.Promise
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.groovy.test.handling.GroovyRequestFixture
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.test.internal.RatpackGroovyDslSpec
import rx.Observable
import rx.Subscriber
import rx.exceptions.CompositeException
import rx.exceptions.OnCompletedFailedException
import rx.exceptions.OnErrorFailedException
import rx.exceptions.OnErrorNotImplementedException
import rx.schedulers.Schedulers

import java.util.concurrent.Executors

class RxErrorHandlingSpec extends RatpackGroovyDslSpec {

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
    RxRatpack.initialize()
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
        Promise.error(error).observe().subscribe {
          render "got to end"
        }
      }
    }

    then:
    thrownException == error
  }

  def "observable sequence without error handler fulfills with Error subclass"() {
    when:
    def e = new Error("Error")
    handlers {
      get {
        Promise.error(e).observe().subscribe {
          render "got to end"
        }
      }
    }

    then:
    thrownException == e
  }

  def "no error handler for successful promise that throws"() {
    when:
    handlers {
      get {
        Promise.value("").observe().subscribe {
          throw error
        }
      }
    }

    then:
    thrownException == error
  }

  def "no error handler for successful promise that has nested failing promise"() {
    when:
    handlers {
      get {
        Promise.value("").observe().subscribe {
          Promise.error(error).observe().subscribe {
            render "got to end"
          }
        }
      }
    }

    then:
    thrownException == error
  }

  def "no error handler for successful promise that has nested successful promise that throws"() {
    when:
    handlers {
      get {
        Promise.value("").observe().subscribe {
          Promise.value("").observe().subscribe {
            throw error
          }
        }
      }
    }

    then:
    thrownException == error
  }

  def "error handler receives promise error"() {
    when:
    def otherError = new RuntimeException("other")
    handlers {
      get {
        Promise.error(error).observe().subscribe {
          render "success"
        } {
          throw otherError
        }
      }
    }

    then:
    def t = thrownException
    t instanceof RuntimeException
    // t.suppressed.length == 1
  }

  def "subscription without error handler results in error forwarded to context error handler"() {
    given:
    def e = new Exception("!")

    when:
    handlers {
      get {
        Observable.error(e).subscribe()
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
        Observable.error(e).map { 2 }.subscribe()
      }
    }

    then:
    get()
    thrownException == e
  }

  def "subscriber can throw"() {
    given:
    def e = new Exception("!")

    when:
    handlers {
      get {
        Observable.just("foo").subscribe {
          throw e
        }
      }
    }

    then:
    get()
    thrownException == e
  }

  def "downstream handler can error"() {
    given:
    def e = new Exception("!")

    when:
    handlers {
      get {
        Observable.just("foo").subscribe {
          next()
        }
      }
      all {
        throw e
      }
    }

    then:
    get()
    thrownException == e
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
        get { Observable.error(new Exception("1")).subscribe() }
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
        get { Observable.error(new Exception("2")).subscribe() }
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
    def result = GroovyRequestFixture.handle({ Observable.error(e).subscribe() } as Handler) {

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
            Observable.error(error).subscribe(new Subscriber() {
              @Override
              void onCompleted() {

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
        Observable.just(1).
          flatMap { Observable.error(e) }.
          subscribe { render "onNext" }
      }
    }

    then:
    thrownException == e
  }

  def "exception thrown by oncomplete is propagated"() {
    when:
    handlers {
      get {
        Observable.just(1).
          subscribe(new Subscriber<Integer>() {
            @Override
            void onCompleted() {
              throw error
            }

            @Override
            void onError(Throwable t) {
              throw new OnErrorNotImplementedException(t)
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
    e instanceof OnCompletedFailedException
    e.cause == error
  }

  def "exception thrown by onerror are propagated"() {
    when:
    handlers {
      get {
        Observable.error(new RuntimeException("1")).
          subscribe(new Subscriber<Integer>() {
            @Override
            void onCompleted() {
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
    e instanceof OnErrorFailedException
    e.cause instanceof CompositeException
    (e.cause as CompositeException).exceptions.first().message == "1"
    (e.cause as CompositeException).exceptions[1] == error
  }

  def "exceptions from bound observable are propagated"() {
    when:
    handlers {
      get {
        Observable.error(error).subscribeOn(Schedulers.from(Executors.newSingleThreadExecutor())).bindExec().promise()
          .onError { render it.toString() }
          .then { render "no error" }
      }
    }

    then:
    text == error.toString()
  }
}
