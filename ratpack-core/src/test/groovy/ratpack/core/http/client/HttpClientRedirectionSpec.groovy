/*
 * Copyright 2016 the original author or authors.
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

package ratpack.core.http.client

import ratpack.exec.Execution
import ratpack.core.http.internal.HttpHeaderConstants
import io.netty.handler.codec.http.HttpResponseStatus
import ratpack.func.Action
import ratpack.http.client.BaseHttpClientSpec

import java.time.Duration

class HttpClientRedirectionSpec extends BaseHttpClientSpec {

  def "can follow simple redirect get request"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
    otherApp {
      get("foo2") {
        redirect(302, otherAppUrl("foo").toString())
      }

      get("foo") {
        render "bar"
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.get(otherAppUrl("foo2")) {
        } then {
          render it.body.text
        }
      }
    }

    then:
    text == "bar"

    where:
    pooled << [true, false]
  }

  def "can follow redirect get request with query parameters"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
    otherApp {
      get("foo2") {
        redirect(301, otherAppUrl("foo?key1=value1&key2=value2").toString())
      }

      get("foo") {
        render context.request.queryParams["key1"] + context.request.queryParams["key2"]
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.get(otherAppUrl("foo2")) {
        } then {
          render it.body.text
        }
      }
    }

    then:
    text == "value1value2"

    where:
    pooled << [true, false]
  }

  def "can follow redirect get request with encoded query parameters"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
    otherApp {
      get("foo2") {
        redirect(301, otherAppUrl("foo?loc=${URLEncoder.encode("http://ratpack.io=10", "UTF-8")}"))
      }

      get("foo") {
        render request.queryParams["loc"]
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.get(otherAppUrl("foo2")) then { render it.body.text }
      }
    }

    then:
    text == "http://ratpack.io=10"

    where:
    pooled << [true, false]
  }

  def "can follow a relative redirect get request with query parameters"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
    otherApp {
      get("foo") {
        response.with {
          status(301)
          headers.set(HttpHeaderConstants.LOCATION, "/tar?key1=value1&key2=value2")
          send()
        }
      }
      get("tar") {
        render context.request.queryParams["key1"] + context.request.queryParams["key2"]
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.get(otherAppUrl("foo")) {
        } then { ReceivedResponse response ->
          render response.body.text
        }
      }
    }

    then:
    text == "value1value2"

    where:
    pooled << [true, false]
  }

  def "can follow a relative redirect get request with encoded query parameters"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
    otherApp {
      get("foo") {
        response.with {
          status(301)
          headers.set(HttpHeaderConstants.LOCATION, "/bar?loc=${URLEncoder.encode("http://ratpack.io=10", "UTF-8")}")
          send()
        }
      }
      get("bar") {
        render request.queryParams["loc"]
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.get(otherAppUrl("foo")) {
        } then { ReceivedResponse response ->
          render response.body.text
        }
      }
    }

    then:
    text == "http://ratpack.io=10"

    where:
    pooled << [true, false]
  }

  def "can follow a relative redirect get request"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
    otherApp {
      get("foo") {
        response.with {
          status(301)
          headers.set(HttpHeaderConstants.LOCATION, "/tar")
          send()
        }
      }
      get("tar") { render "tar" }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.get(otherAppUrl("foo")) {
        } then { ReceivedResponse response ->
          render response.body.text
        }
      }
    }

    then:
    text == "tar"

    where:
    pooled << [true, false]
  }


  def "Do not follow simple redirect if redirects set to 0"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
    otherApp {
      get("foo2") {
        redirect(302, otherAppUrl("foo").toString())
      }

      get("foo") {
        render "bar"
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.get(otherAppUrl("foo2")) {
          it.redirects(0)
        } then { ReceivedResponse response ->
          render response.body.text
        }
      }
    }

    then:
    text == ""

    where:
    pooled << [true, false]
  }

  def "Stop redirects in loop"() {
    given:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
    otherApp {
      get("foo2") {
        redirect(302, otherAppUrl("foo").toString())
      }

      get("foo") {
        redirect(302, otherAppUrl("foo2").toString())
      }
    }

    when:
    handlers {
      get { HttpClient httpClient ->
        httpClient.get(otherAppUrl("foo2")) {} then { ReceivedResponse response ->
          render "Status: " + response.statusCode
        }
      }
    }

    then:
    text == "Status: 302"

    where:
    pooled << [true, false]
  }

  def "can use redirect strategy"() {
    when:
    bindings {
      bindInstance(HttpClient, HttpClient.of { it.poolSize(pooled ? 8 : 0) })
    }
    otherApp {
      get {
        def count = request.headers.get("count").toInteger()
        if (count < 5) {
          response.headers.set("count", count.toString())
          redirect "/"
        } else {
          render count.toString()
        }
      }
    }

    handlers {
      get { HttpClient httpClient ->
        httpClient.get(otherAppUrl()) { RequestSpec r ->
          r.headers.set("count", "0")
          r.onRedirect { ReceivedResponse res ->
            def count = res.headers.get("count").toInteger()
            return { it.headers.set("count", count + 1) } as Action
          }
        }.then {
          render it.body.text
        }
      }
    }

    then:
    text == "5"

    where:
    pooled << [true, false]
  }

  def "redirect strategy can access execution"() {
    when:
    otherApp {
      get {
        execution.sleep(Duration.ofSeconds(1)) {
          redirect "/other"
        }
      }
      get("other") {
        render header("value").get()
      }
    }

    handlers {
      get { HttpClient httpClient ->
        Execution.current().add("foo")
        httpClient.get(otherAppUrl()) { RequestSpec r ->
          r.onRedirect { ReceivedResponse res ->
            return { it.headers.set("value", Execution.current().get(String)) } as Action
          }
        }.then {
          render it.body.text
        }
      }
    }

    then:
    text == "foo"
  }

  def "can follow a redirect when sending a large request"() {
    when:
    otherApp {
      post("a") {
        request.maxContentLength = Long.MAX_VALUE
        onClose {
          println "a finished"
        }
        redirect 303, "b"
      }
      post("b") {
        request.maxContentLength = Long.MAX_VALUE
        onClose {
          println "b finished"
        }
        render "ok"
      }
    }

    handlers {
      get {
        get(HttpClient).post(otherAppUrl("a")) {
          it.body.text("a" * 1024 * 1024 * 10)
        }.then {
          render it.body.text
        }
      }
    }

    then:
    getText() == "ok"
  }

  def "Should set cookies from redirect"() {
    given:
    requestSpec { r -> r.redirects(1) }

    and:
    handlers {
      get {
        response.send(request.oneCookie("value") ?: 'none')
      }
      get(':cookie') {
        response.cookie('value', pathTokens.cookie)
        redirect '/'
      }
    }

    when:
    get()

    then:
    response.statusCode == HttpResponseStatus.OK.code()
    response.body.text == 'none'

    when:
    get('Ratpack')

    then:
    response.statusCode == HttpResponseStatus.OK.code()
    response.body.text == 'Ratpack'

    when:
    get()

    then:
    response.statusCode == HttpResponseStatus.OK.code()
    response.body.text == 'Ratpack'
  }

  def "handles relative redirects"() {
    when:
    handlers {
      get("to") {
        render "ok1"
      }
      get("abs-path") {
        redirect "/to"
      }
      get("rel-path") {
        redirect "to"
      }
      get("rel-path/to") {
        render "ok2"
      }
      get("rel-protocol") {
        redirect "//localhost:$server.bindPort/to"
      }
    }

    then:
    getText("abs-path") == "ok1"
    getText("rel-path") == "ok1"
    getText("rel-path/") == "ok2"
    getText("rel-protocol") == "ok1"
  }

}
