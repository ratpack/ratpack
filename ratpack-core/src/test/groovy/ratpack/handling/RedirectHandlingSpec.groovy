/*
 * Copyright 2013 the original author or authors.
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
package ratpack.handling

import io.netty.handler.codec.http.HttpResponseStatus
import ratpack.http.MutableHeaders
import ratpack.http.client.RequestSpec
import ratpack.test.internal.RatpackGroovyDslSpec

class RedirectHandlingSpec extends RatpackGroovyDslSpec {

  @Override
  void configureRequest(RequestSpec requestSpec){
    requestSpec.redirects 0
  }

  def "Absolute Path Redirect"() {
    when:
    handlers {
      get {
        redirect("http://www.google.com")
      }
    }

    then:
    def resp = get("")
    resp.statusCode == 302
    resp.headers.get("Location") == "http://www.google.com"
  }

  def "Server Root Path Redirect no public url"() {
    when:
    handlers {
      get {
        redirect("/index")
      }
    }

    then:
    def resp = get("")
    resp.statusCode == 302
    resp.headers.get("Location") == "http://${server.bindHost}:${server.bindPort}/index"
  }

  def "Server Relative Path Redirect no public url"() {
    when:
    handlers {
      get("index") {
        redirect("other")
      }
    }

    then:
    def resp = get("index")
    resp.statusCode == 302
    resp.headers.get("Location") == "http://${server.bindHost}:${server.bindPort}/other"
  }

  def "Server root path redirect with public url"() {
    when:
    def publicUrl = "http://example.com"
    serverConfig {
      publicAddress(new URI(publicUrl))
    }

    handlers {
      get {
        redirect("/index")
      }
    }

    when:

    then:
    def resp = get("")
    resp.statusCode == 302
    resp.headers.get("Location") == publicUrl + "/index"
  }

  def "Server Relative Path Redirect with public url"() {
    when:
    def publicUrl = "http://example.com"
    serverConfig {
      publicAddress(new URI(publicUrl))
    }

    handlers {
      get("index") {
        redirect("other")
      }
    }

    then:
    def resp = get("index")
    resp.statusCode == 302
    resp.headers.get("Location") == publicUrl + "/other"
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


  def "Protocol relative url"() {
    when:

    handlers {
      get {
        redirect("//google.com/")
      }
    }

    then:
    def resp = get("")
    resp.statusCode == 302
    resp.headers.get("Location") == "http://google.com/"
  }


  def "Protocol relative url with x-forwarded-proto"() {
    when:

    handlers {
      get {
        redirect("//google.com/")
      }
    }

    and:
    requestSpec{ RequestSpec requestSpec ->
      requestSpec.headers { MutableHeaders headers ->
        headers.set("x-forwarded-proto", "https")
      }
    }

    then:
    def resp = get("")
    resp.statusCode == 302
    resp.headers.get("Location") == "https://google.com/"
  }


}
