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
import ratpack.groovy.test.GroovyUnitTest
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.test.internal.RatpackGroovyDslSpec
import rx.Observable
import rx.Subscriber
import rx.exceptions.CompositeException
import rx.exceptions.OnErrorNotImplementedException
import rx.functions.Action0
import rx.functions.Action1

import static ratpack.groovy.test.TestHttpClients.testHttpClient
import static ratpack.groovy.test.embed.EmbeddedApplications.embeddedApp
import static ratpack.rx.RxRatpack.observe
import static ratpack.rx.RxRatpack.subscriber

class RxErrorHandlingSpec extends RatpackGroovyDslSpec {

  static class MessagePrintingErrorHandler implements ServerErrorHandler {
    List<Exception> errors = []

    @Override
    void error(Context context, Throwable throwable) throws Throwable {
      errors << throwable
      context.render("threw throwable")
    }
  }

  def errorHandler = new MessagePrintingErrorHandler()
  def error = new RuntimeException("!")

  def setup() {
    RxRatpack.initialize()
    bindings {
      bind ServerErrorHandler, errorHandler
    }
  }

  Exception getThrownException() {
    assert text == "threw throwable"
    errorHandler.errors.last()
  }

  def "no error handler for failed promise"() {
    when:
    handlers {
      get {
        observe(promise({
          it.error(error)
        })) subscribe {
          render "got to end"
        }
      }
    }

    then:
    thrownException == error
  }

  def "no error handler for successful promise that throws"() {
    when:
    handlers {
      get {
        observe(promise({
          it.success("")
        })) subscribe {
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
        observe(promise({
          it.success("")
        })) subscribe {
          observe(promise({
            it.error(error)
          })) subscribe {
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
        observe(promise({
          it.success("")
        })) subscribe {
          observe(promise({
            it.success("")
          })) subscribe {
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
        observe(promise({
          it.error(error)
        })) subscribe({
          render "success"
        }, {
          throw otherError
        } as Action1)
      }
    }

    CompositeException cause = thrownException.cause as CompositeException

    then:
    cause.exceptions == [error, otherError]
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

  def "on complete can throw"() {
    given:
    def e = new Exception("!")

    when:
    handlers {
      get {
        Observable.just("foo").subscribe({}, { error(it as Exception) }, { throw e } as Action0)
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
      handler {
        throw e
      }
    }

    then:
    get()
    thrownException == e
  }

  def "can use two different rx ratpack apps in same jvm"() {
    when:
    def app1 = embeddedApp {
      bindings {
        bind ServerErrorHandler, new ServerErrorHandler() {
          @Override
          void error(Context context, Throwable throwable) throws Throwable {
            context.render "app1"
          }
        }
      }
      handlers {
        get { Observable.error(new Exception("1")).subscribe() }
      }
    }
    def client1 = testHttpClient(app1)
    def app2 = embeddedApp {
      bindings {
        bind ServerErrorHandler, new ServerErrorHandler() {
          @Override
          void error(Context context, Throwable throwable) throws Throwable {
            context.render "app2"
          }
        }
      }
      handlers {
        get { Observable.error(new Exception("2")).subscribe() }
      }
    }
    def client2 = testHttpClient(app2)

    app2.server.start()

    then:
    client1.text == "app1"
    client2.text == "app2"
  }

  def "can use rx in a unit test"() {
    given:
    def e = new Exception("!")

    when:
    def result = GroovyUnitTest.handle({ Observable.error(e).subscribe() } as Handler) {

    }

    then:
    result.throwable == e
  }

  def "error handler is invoked even when error occurs on different thread"() {
    given:
    handlers {
      get { ExecController execController ->
        promise { f ->
          execController.executor.execute {
            Observable.error(error).subscribe(subscriber(f))
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

  def "exception thrown by oncomplete throwns"() {
    given:
    def e = new Exception("!")

    when:
    handlers {
      get {
        Observable.just(1).
          subscribe(new Subscriber<Integer>() {
            @Override
            void onCompleted() {
              throw e
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
    thrownException == e
  }

}
