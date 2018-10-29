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
import ratpack.error.ServerErrorHandler
import ratpack.error.internal.DefaultDevelopmentErrorHandler
import ratpack.exec.Promise
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.handling.Context
import ratpack.http.client.HttpClient
import ratpack.http.client.ReceivedResponse
import ratpack.registry.RegistrySpec
import ratpack.server.ServerConfig
import ratpack.test.embed.EmbeddedApp
import ratpack.test.exec.ExecHarness
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import spock.lang.AutoCleanup
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration

class RatpackRetrofitSpec extends Specification {

  interface Service {
    @GET("/")
    Promise<String> root()

    @GET("/")
    Promise<Response<String>> rootResponse()

    @GET("/")
    Promise<ReceivedResponse> rawResponse()

    @GET("/foo")
    Promise<Response<String>> fooResponse()

    @POST("/bar")
    Promise<Response<String>> barResponse()

    @GET("/error")
    Promise<String> error()

    @GET("/error")
    Promise<Response<String>> errorResponse()
  }

  Service service

  @AutoCleanup
  EmbeddedApp server

  Context mockContext = Mock()

  HttpClient client() {
    HttpClient.of {
      it.byteBufAllocator(UnpooledByteBufAllocator.DEFAULT)
      it.maxContentLength(ServerConfig.DEFAULT_MAX_CONTENT_LENGTH)
      it.connectTimeout(Duration.ofMillis(3000))
      it.readTimeout(Duration.ofMillis(3000))
    }
  }

  def setup = { RegistrySpec r ->
    r.add(mockContext)
  }

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
        post("bar") {
          response.status(200).send()
        }
      }
    }
    service = RatpackRetrofit
      .client(server.address)
      .build(Service)
    _ * mockContext.get(HttpClient) >> { client() }
  }

  def "successful request body"() {
    when:
    def value = ExecHarness.yieldSingle(setup) {
      service.root()
    }.valueOrThrow

    then:
    value == "OK"
  }

  def "successful request response"() {
    when:
    Response<String> value = ExecHarness.yieldSingle(setup) {
      service.fooResponse()
    }.valueOrThrow

    then:
    value.code() == 200
    value.body() == "foo"
  }

  def "successful empty body post request response"() {
    when:
    Response<String> value = ExecHarness.yieldSingle(setup) {
      service.barResponse()
    }.valueOrThrow

    then:
    value.code() == 200
    value.body() == ""
  }

  def "simple type adapter throws exception on non successful response"() {
    when:
    ExecHarness.yieldSingle(setup) {
      service.error()
    }.valueOrThrow

    then:
    def t = thrown(RatpackRetrofitCallException)
    t.response.statusCode == 500

  }

  def "response type adapter does not throw on non successful response"() {

    when:
    Response<String> response = ExecHarness.yieldSingle(setup) {
      service.errorResponse()
    }.valueOrThrow

    then:
    noExceptionThrown()

    !response.isSuccessful()
  }

  def "can adapt to ReceivedResponse"() {
    when:
    String response = ExecHarness.yieldSingle(setup) {
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
    ExecHarness.yieldSingle(setup) {
      service.rootResponse()
    }.valueOrThrow

    then:
    thrown(ConnectException)

  }

  def "get client from execution"() {
    when:
    def value = ExecHarness.yieldSingle { r ->
      r.add(client())
    } {
      service.root()
    }.valueOrThrow

    then:
    value == "OK"
  }

  @Unroll
  def "test client connect timeout - #connectTimeout"() {
    given:
    def builder = RatpackRetrofit
      .client("http://ratpack.io:65535")
      .httpClient {
        return client()
      }
    if (connectTimeout != null) {
      builder.httpClient {
        HttpClient.of {
          it.byteBufAllocator(UnpooledByteBufAllocator.DEFAULT)
          it.maxContentLength(ServerConfig.DEFAULT_MAX_CONTENT_LENGTH)
          it.connectTimeout(Duration.ofMillis(connectTimeout))
          it.readTimeout(Duration.ofMillis(3000))
        }
      }
    }
    service = builder.build(Service)

    when:
    ExecHarness.yieldSingle(setup) {
      service.rootResponse()
    }.valueOrThrow

    then:
    def e = thrown(ConnectException)
    if (connectTimeout != null) {
      assert e.message == "Connect timeout (PT1S) connecting to http://ratpack.io:65535/"
    } else {
      assert e.message == "Connect timeout (PT3S) connecting to http://ratpack.io:65535/"
    }

    where:
    connectTimeout << [1000, null]
  }

  @Unroll
  def "test client read timeout - #readTimeout"() {
    given:
    def otherApp = GroovyEmbeddedApp.of {
      registryOf { add ServerErrorHandler, new DefaultDevelopmentErrorHandler() }
      handlers {
        get {
          sleep 3000
          render "OK"
        }
      }
    }

    and:
    def builder = RatpackRetrofit
      .client(otherApp.address)
      .httpClient {
        return client()
      }
    if (readTimeout != null) {
      builder.httpClient {
        HttpClient.of {
          it.byteBufAllocator(UnpooledByteBufAllocator.DEFAULT)
          it.maxContentLength(ServerConfig.DEFAULT_MAX_CONTENT_LENGTH)
          it.connectTimeout(Duration.ofMillis(3000))
          it.readTimeout(Duration.ofMillis(readTimeout))
        }
      }
    }
    service = builder.build(Service)

    when:
    ExecHarness.yieldSingle(setup) {
      service.rootResponse()
    }.valueOrThrow

    then:
    def e = thrown(IOException)
    if (readTimeout != null) {
      assert e.message == "ratpack.http.client.HttpClientReadTimeoutException: Read timeout (PT1S) waiting on HTTP server at $otherApp.address"
    } else {
      assert e.message == "ratpack.http.client.HttpClientReadTimeoutException: Read timeout (PT3S) waiting on HTTP server at $otherApp.address"
    }
    where:
    readTimeout << [1000, null]
  }
}
