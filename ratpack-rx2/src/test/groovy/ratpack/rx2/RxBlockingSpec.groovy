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

package ratpack.rx2

import io.reactivex.functions.Action
import io.reactivex.functions.Consumer
import ratpack.error.ServerErrorHandler
import ratpack.exec.Blocking
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.test.internal.SimpleErrorHandler

import static ratpack.rx2.RxRatpack.observe
import static ratpack.rx2.RxRatpack.single

class RxBlockingSpec extends RatpackGroovyDslSpec {

  def setup() {
    RxRatpack.initialize()
  }

  def "can observe the blocking"() {
    when:
    handlers {
      get(":value") {
        single(Blocking.get {
          pathTokens.value
        }) map {
          it * 2
        } map {
          it.toUpperCase()
        } subscribe({
          render it
        } as Consumer)
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
        single(Blocking.get {
          pathTokens.value
        }) map {
          it * 2
        } map {
          throw new Exception("!!!!")
        } subscribe({
          render "shouldn't happen"
        } as Consumer)
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
        single(Blocking.get {
          pathTokens.value
        }) map {
          it * 2
        } map {
          throw new Exception("!!!!")
        } subscribe({
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

        observe(Blocking.get {
          pathTokens.value.split(",") as List
        })
          .take(2)
          .map { it.toLowerCase() }
          .subscribe({
          returnString += it
        }, { Throwable error ->
          throw error
        }, {
          render returnString
        } as Action)
      }
    }

    then:
    getText("A,B,C") == "ab"
  }
}

