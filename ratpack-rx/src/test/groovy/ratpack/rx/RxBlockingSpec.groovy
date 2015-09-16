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
import ratpack.exec.Blocking
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.test.internal.SimpleErrorHandler
import rx.functions.Action0

import static ratpack.rx.RxRatpack.observe
import static ratpack.rx.RxRatpack.observeEach

class RxBlockingSpec extends RatpackGroovyDslSpec {

  def setup() {
    RxRatpack.initialize()
  }

  def "can observe the blocking"() {
    when:
    handlers {
      get(":value") {
        observe(Blocking.get {
          pathTokens.value
        }) map {
          it * 2
        } map {
          it.toUpperCase()
        } subscribe {
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
        observe(Blocking.get {
          pathTokens.value
        }) map {
          it * 2
        } map {
          throw new Exception("!!!!")
        } subscribe {
          render "shouldn't happen"
        }
      }
    }

    then:
    getText("a").startsWith new Exception("!!!!").toString()
    response.statusCode == 500
  }

  def "can observe the blocking operation with an Iterable return type"() {
    when:
    handlers {
      get(":value") {
        def returnString = ""

        observeEach(Blocking.get {
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
        } as Action0)
      }
    }

    then:
    getText("A,B,C") == "ab"
  }
}

