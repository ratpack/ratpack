/*
 * Copyright 2015 the original author or authors.
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

package ratpack.retrofit

import io.netty.buffer.UnpooledByteBufAllocator
import ratpack.exec.Promise
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.http.client.HttpClient
import ratpack.http.client.ReceivedResponse
import ratpack.http.client.internal.DefaultHttpClient
import ratpack.registry.RegistrySpec
import ratpack.server.ServerConfig
import ratpack.test.embed.EmbeddedApp
import ratpack.test.exec.ExecHarness
import retrofit2.Response
import retrofit2.http.GET
import spock.lang.AutoCleanup
import spock.lang.Specification

class RatpackRetrofitSpec extends Specification {

  interface Service {
    @GET("/") Promise<String> root()
    @GET("/") Promise<Response<String>> rootResponse()
    @GET("/") Promise<ReceivedResponse> rawResponse()
    @GET("/foo") Promise<Response<String>> fooResponse()
    @GET("/error") Promise<String> error()
    @GET("/error") Promise<Response<String>> errorResponse()
  }

  Service service

  @AutoCleanup
  EmbeddedApp server

  HttpClient client = new DefaultHttpClient(UnpooledByteBufAllocator.DEFAULT, ServerConfig.DEFAULT_MAX_CONTENT_LENGTH)

  def setup() {
    server = GroovyEmbeddedApp.of {
      handlers {
        get {
          render "OK"
        }
        get("foo") {
          render "foo"
        }
        get("error") {
          response.status(500).send()
        }
      }
    }
    service = RatpackRetrofit
      .client(server.address)
      .build(Service)
  }

  def "successful request body"() {
    when:
    def value = ExecHarness.yieldSingle({ RegistrySpec r ->
      r.add(client)
    }) {
      service.root()
    }.valueOrThrow

    then:
    value == "OK"
  }

  def "successful request response"() {
    when:
    Response<String> value = ExecHarness.yieldSingle({ RegistrySpec r ->
      r.add(client)
    }) {
      service.fooResponse()
    }.valueOrThrow

    then:
    value.code() == 200
    value.body() == "foo"
  }

  def "simple type adapter throws exception on non successful response"() {
    when:
    ExecHarness.yieldSingle({ RegistrySpec r ->
      r.add(client)
    }) {
      service.error()
    }.valueOrThrow

    then:
    thrown(RatpackRetrofitCallException)

  }

  def "response type adapter does not throw on non successful response"() {

    when:
    Response<String> response = ExecHarness.yieldSingle({ RegistrySpec r ->
      r.add(client)
    }) {
      service.errorResponse()
    }.valueOrThrow

    then:
    noExceptionThrown()

    !response.isSuccessful()
  }

  def "can adapt to ReceivedResponse"() {
    when:
    String response = ExecHarness.yieldSingle({ RegistrySpec r ->
      r.add(client)
    }) {
      service.rawResponse().map {
        "${it.statusCode}:${it.body.text}"
      }
    }.valueOrThrow

    then:
    response == "200:OK"
  }

  def "exception thrown on connection exceptions"() {
    given:
    service = RatpackRetrofit
      .client("http://localhost:8080")
      .build(Service)

    when:
    ExecHarness.yieldSingle({ RegistrySpec r ->
      r.add(client)
    }) {
      service.rootResponse()
    }.valueOrThrow

    then:
    thrown(ConnectException)

  }
}
