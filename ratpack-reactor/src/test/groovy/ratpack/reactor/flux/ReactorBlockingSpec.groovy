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

import ratpack.error.ServerErrorHandler
import ratpack.exec.Blocking
import ratpack.reactor.ReactorRatpack
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.test.internal.SimpleErrorHandler

import java.util.function.Consumer
import java.util.function.Function

import static ratpack.reactor.ReactorRatpack.flux
import static ratpack.reactor.ReactorRatpack.fluxEach

class ReactorBlockingSpec extends RatpackGroovyDslSpec {

  def setup() {
    ReactorRatpack.initialize()
  }

  def "can observe the blocking"() {
    when:
    handlers {
      get(":value") {
        flux(Blocking.get {
          pathTokens.value
        }) map({
          it * 2
        } as Function) map({
          it.toString().toUpperCase()
        } as Function) subscribe {
          render it
        }
      }
    }

    then:
    getText("a") == "AA"
  }

  def "blocking errors are sent to the context renderer"() {
    when:
    bindings {
      bind ServerErrorHandler, SimpleErrorHandler
    }
    handlers {
      get(":value") {
        flux(Blocking.get {
          pathTokens.value
        }) map({
          it * 2
        } as Function) map({
          throw new Exception("!!!!")
        } as Function) subscribe{
          render "shouldn't happen"
        }
      }
    }

    then:
    getText("a").startsWith new Exception("!!!!").toString()
    response.statusCode == 500
  }

  def "blocking errors can be caught by onerror"() {
    when:
    bindings {
      bind ServerErrorHandler, SimpleErrorHandler
    }
    handlers {
      get(":value") {
        flux(Blocking.get {
          pathTokens.value
        }) map({
          it * 2
        } as Function) map({
          throw new Exception("!!!!")
        } as Function) subscribe({
          render "shouldn't happen"
        }, { render "b" })
      }
    }

    then:
    getText("a") == "b"
  }

  def "can observe the blocking operation with an Iterable return type"() {
    when:
    handlers {
      get(":value") {
        def returnString = ""

        fluxEach(Blocking.get {
          pathTokens.value.split(",") as List
        })
          .take(2)
          .map({ it.toLowerCase() } as Function)
          .subscribe({
          returnString += it
        } as Consumer, { Throwable error ->
          throw error
        } as Consumer, {
          render returnString
        } as Runnable)
      }
    }

    then:
    getText("A,B,C") == "ab"
  }
}

