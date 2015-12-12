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

import com.squareup.okhttp.MediaType
import com.squareup.okhttp.RequestBody
import com.squareup.okhttp.ResponseBody
import ratpack.exec.Promise
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.test.embed.EmbeddedApp
import ratpack.test.exec.ExecHarness
import retrofit.Converter
import retrofit.Response
import retrofit.Retrofit
import retrofit.http.GET
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.lang.annotation.Annotation
import java.lang.reflect.Type


class RatpackCallAdapterFactorySpec extends Specification {

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
    retrofit = new Retrofit.Builder()
      .baseUrl(server.address.toString())
      .addConverterFactory(new StringConverterFactory())
      .addCallAdapterFactory(RatpackCallAdapterFactory.INSTANCE)
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
    public Converter<ResponseBody, ?> fromResponseBody(Type type, Annotation[] annotations) {
      return new Converter<ResponseBody, String>() {
        @Override public String convert(ResponseBody value) throws IOException {
          return value.string();
        }
      };
    }

    @Override public Converter<?, RequestBody> toRequestBody(Type type,
                                                                    Annotation[] annotations) {
      return new Converter<String, RequestBody>() {
        @Override public RequestBody convert(String value) throws IOException {
          return RequestBody.create(MediaType.parse("text/plain"), value);
        }
      };
    }
  }
}
