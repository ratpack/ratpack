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
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import ratpack.exec.Promise
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.http.client.internal.DefaultHttpClient
import ratpack.server.ServerConfig
import ratpack.test.embed.EmbeddedApp
import ratpack.test.exec.ExecHarness
import retrofit2.Converter
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.lang.annotation.Annotation
import java.lang.reflect.Type


class RatpackRetrofitSpec extends Specification {

  interface Service {
    @GET("/") Promise<String> promiseBody()
    @GET("/") Promise<Response<String>> promiseResponse()
  }

  Retrofit retrofit
  Service service

  @AutoCleanup
  EmbeddedApp server

  def setup() {
    server = GroovyEmbeddedApp.of {
      handlers {
        get {
          render "OK"
        }
      }
    }
    retrofit = RatpackRetrofit.builder(new DefaultHttpClient(UnpooledByteBufAllocator.DEFAULT, ServerConfig.DEFAULT_MAX_CONTENT_LENGTH))
      .uri(server.address)
      .configure { Retrofit.Builder b ->
        b.addConverterFactory(new StringConverterFactory())
      }
      .build()
    service = retrofit.create(Service)
  }

  def "successful request body"() {
    when:
    def value = ExecHarness.yieldSingle {
      service.promiseBody()
    }.valueOrThrow

    then:
    value == "OK"
  }

  def "successful request response"() {
    when:
    Response<String> value = ExecHarness.yieldSingle {
      service.promiseResponse()
    }.valueOrThrow

    then:
    value.code() == 200
    value.body() == "OK"
  }

  static class StringConverterFactory extends Converter.Factory {
    @Override
    Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
      return new Converter<ResponseBody, String>() {
        @Override public String convert(ResponseBody value) throws IOException {
          return value.string();
        }
      };
    }

    @Override
    Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
      return new Converter<String, RequestBody>() {
        @Override public RequestBody convert(String value) throws IOException {
          return RequestBody.create(MediaType.parse("text/plain"), value);
        }
      };
    }
  }
}
