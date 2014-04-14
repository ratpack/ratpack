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

import com.google.inject.AbstractModule
import ratpack.error.ServerErrorHandler
import ratpack.groovy.internal.ClosureUtil
import ratpack.handling.Context
import ratpack.exec.Fulfiller
import ratpack.test.internal.RatpackGroovyDslSpec
import rx.exceptions.CompositeException

import java.util.concurrent.TimeUnit

import static ratpack.rx.RxRatpack.observe

class RxErrorHandlingSpec extends RatpackGroovyDslSpec {

  static class MessagePrintingErrorHandler implements ServerErrorHandler {
    List<Exception> errors = []

    @Override
    void error(Context context, Exception exception) throws Exception {
      errors << exception
      context.render("threw exception")
    }
  }

  def errorHandler = new MessagePrintingErrorHandler()
  def error = new RuntimeException("!")

  def setup() {
    modules << new AbstractModule() {
      @Override
      protected void configure() {
        bind(ServerErrorHandler).toInstance(errorHandler)
      }
    }
  }

  Exception getThrownException() {
    assert text == "threw exception"
    errorHandler.errors.last()
  }

  def "no error handler for failed promise"() {
    when:
    handlers {
      get {
        observe(promise({
          defer it, { error(error) }
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
          defer it, { success("") }
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
          defer it, { success("") }
        })) subscribe {
          observe(promise({
            defer it, { error(error) }
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
          defer it, { success("") }
        })) subscribe {
          observe(promise({
            defer it, { success("") }
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
    handlers {
      get {
        observe(promise({
          defer it, { error(error) }
        })) subscribe({
          render "success"
        }, {
          throw error
        })
      }
    }

    CompositeException cause = thrownException.cause as CompositeException

    then:
    cause.exceptions == [error, error]
  }

  void defer(object, @DelegatesTo(Fulfiller) Closure<?> closure) {
    application.server.launchConfig.execController.executor.schedule({ ClosureUtil.configureDelegateFirst(object, closure) }, 1, TimeUnit.MILLISECONDS)
  }

}
