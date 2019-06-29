/*
 * Copyright 2019 the original author or authors.
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

package ratpack.test.mock

import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.http.HttpMethod
import ratpack.http.HttpUrlBuilder
import ratpack.http.Request
import ratpack.http.client.HttpClient
import ratpack.test.embed.EmbeddedApp
import ratpack.test.handling.HandlerFactory
import spock.lang.Specification

import static ratpack.groovy.Groovy.groovyHandler

class MockApiSpec extends Specification {

  def "can mock a remote api"() {
    given:
    HandlerFactory factory = { Request request ->
      if (request.method == HttpMethod.GET) {
        return groovyHandler {
          render("get on remote API")
        }
      }
      return groovyHandler {
        response.status(400).send()
      }
    } as HandlerFactory

    MockApi remoteApi = MockApi.of(factory)

    EmbeddedApp app = GroovyEmbeddedApp.fromHandlers {
      get("get") { ctx ->
        get(HttpClient).get(remoteApi.getAddress()).then {
          it.forwardTo(ctx.getResponse())
        }
      }
      get("post") { ctx ->
        get(HttpClient).post(remoteApi.getAddress()) {}.then {
          it.forwardTo(ctx.getResponse())
        }
      }
    }
    def client = app.getHttpClient()

    expect:
    client.get("get").body.text == "get on remote API"
    client.get("post").statusCode == 400

    cleanup:
    remoteApi?.close()
  }

  def "can mock a remote api using Spock mocks"() {
    given:
    MockApi remoteApi = MockApi.of(Mock(HandlerFactory))

    EmbeddedApp app = GroovyEmbeddedApp.fromHandlers {
      get("get") { ctx ->
        get(HttpClient).get(
          HttpUrlBuilder.base(remoteApi.address)
            .path(ctx.request.path)
            .build()
        ).then {
          it.forwardTo(ctx.getResponse())
        }
      }
      get("post") { ctx ->
        get(HttpClient).post(
          HttpUrlBuilder.base(remoteApi.address)
          .path(ctx.request.path)
          .build()
        ) {}.then {
          it.forwardTo(ctx.getResponse())
        }
      }
    }
    def client = app.getHttpClient()

    when:
    client.get("get").body.text == "get on remote API"

    then:
    1 * remoteApi.handlerFactory.receive({
      it.method == HttpMethod.GET
      it.path == "get"
    }) >> groovyHandler {
      render("get on remote API")
    }

    when:
    client.get("post").statusCode == 400

    then:
    1 * remoteApi.handlerFactory.receive({
      it.method == HttpMethod.POST
      it.path == "post"
    }) >> groovyHandler {
      response.status(400).send()
    }
  }
}
