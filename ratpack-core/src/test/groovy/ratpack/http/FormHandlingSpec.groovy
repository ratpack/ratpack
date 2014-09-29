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

package ratpack.http

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.DefaultFullHttpRequest
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder
import ratpack.error.ServerErrorHandler
import ratpack.error.internal.DefaultDevelopmentErrorHandler
import ratpack.form.Form
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.http.client.RequestSpec
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.Unroll

import static ratpack.http.MediaType.APPLICATION_FORM


class FormHandlingSpec extends RatpackGroovyDslSpec {

  def setup() {
    bindings {
      bind ServerErrorHandler, new DefaultDevelopmentErrorHandler()
    }
  }

  def "can get form params"() {
    when:
    handlers {
      post {
        def form = parse Form
        render form.toString()
      }
    }

    then:
    requestSpec { it.headers.add("Content-Type", APPLICATION_FORM) }
    postText() == "[:]"
    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec ->
      requestSpec.headers.add("Content-Type", APPLICATION_FORM)
      requestSpec.body.stream({ it << [a: "b"].collect({ it }).join('&') })
    }
    then:
    postText() == "[a:[b]]"
    when:
    resetRequest()

    requestSpec { RequestSpec requestSpec ->
      def body = "a=b&a=c&d=e&abc="
      requestSpec.headers.add("Content-Type", APPLICATION_FORM)
      requestSpec.body.stream({ it << body })
    }
    then:
    postText() == "[a:[b, c], d:[e], abc:[]]"
  }

  def "can read multi part forms"() {
    when:
    handlers {
      post {
        def form = parse Form
        render form.toString()
      }
    }

    and:
    requestSpec { RequestSpec requestSpec ->

      HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, io.netty.handler.codec.http.HttpMethod.POST, "/")
      HttpPostRequestEncoder httpPostRequestEncoder = new HttpPostRequestEncoder(request, true)
      httpPostRequestEncoder.addBodyAttribute("foo", "1")
      httpPostRequestEncoder.addBodyAttribute("bar", "2")
      httpPostRequestEncoder.addBodyAttribute("bar", "3")

      request = httpPostRequestEncoder.finalizeRequest()

      request.headers().each {
        requestSpec.headers.set(it.key, it.value)
      }

      def chunks = []
      while (!httpPostRequestEncoder.isEndOfInput()) {
        chunks << httpPostRequestEncoder.readChunk(null).content()
      }
      requestSpec.body.buffer(Unpooled.wrappedBuffer(chunks as ByteBuf[]))
    }

    then:
    postText() == "[foo:[1], bar:[2, 3]]"
  }

  def "can handle file uploads"() {
    given:
    def fooFile = file "foo.txt", "bar"

    when:
    handlers {
      post {
        def form = parse Form
        render "File content: " + form.file("theFile").text
      }
    }

    then:
    requestSpec { RequestSpec requestSpec ->
      HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, io.netty.handler.codec.http.HttpMethod.POST, "/")
      HttpPostRequestEncoder httpPostRequestEncoder = new HttpPostRequestEncoder(request, true)
      httpPostRequestEncoder.addBodyFileUpload("theFile", fooFile.toFile(), "text/plain", true)

      request = httpPostRequestEncoder.finalizeRequest()

      request.headers().each {
        requestSpec.headers.set(it.key, it.value)
      }

      def chunks = []
      while (!httpPostRequestEncoder.isEndOfInput()) {
        chunks << httpPostRequestEncoder.readChunk(null).content()
      }
      requestSpec.body.buffer(Unpooled.wrappedBuffer(chunks as ByteBuf[]))
    }

    postText() == "File content: bar"
  }

  def "default encoding is utf-8"() {
    given:
    def fooFile = file "foo.txt", "bar"

    when:
    handlers {
      post {
        def form = parse Form
        render "File type: " + form.file("theFile").contentType
      }
    }

    then:
    requestSpec { RequestSpec requestSpec ->
      HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, io.netty.handler.codec.http.HttpMethod.POST, "/")
      HttpPostRequestEncoder httpPostRequestEncoder = new HttpPostRequestEncoder(request, true)
      httpPostRequestEncoder.addBodyFileUpload("theFile", fooFile.toFile(), "text/plain", true)

      request = httpPostRequestEncoder.finalizeRequest()

      request.headers().each {
        requestSpec.headers.set(it.key, it.value)
      }

      def chunks = []
      while (!httpPostRequestEncoder.isEndOfInput()) {
        chunks << httpPostRequestEncoder.readChunk(null).content()
      }
      requestSpec.body.buffer(Unpooled.wrappedBuffer(chunks as ByteBuf[]))
    }
    postText() == "File type: text/plain;charset=UTF-8"
  }

  def "respects custom encoding"() {
    given:
    def fooFile = file "foo.txt", "bar"

    when:
    handlers {
      post {
        def form = parse Form
        render "File type: " + form.file("theFile").contentType
      }
    }

    then:
    requestSpec { RequestSpec requestSpec ->
      HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, io.netty.handler.codec.http.HttpMethod.POST, "/")
      HttpPostRequestEncoder httpPostRequestEncoder = new HttpPostRequestEncoder(request, true)
      httpPostRequestEncoder.addBodyFileUpload("theFile", fooFile.toFile(), "text/plain;charset=US-ASCII", false)

      request = httpPostRequestEncoder.finalizeRequest()

      request.headers().each {
        requestSpec.headers.set(it.key, it.value)
      }

      def chunks = []
      while (!httpPostRequestEncoder.isEndOfInput()) {
        chunks << httpPostRequestEncoder.readChunk(null).content()
      }
      requestSpec.body.buffer(Unpooled.wrappedBuffer(chunks as ByteBuf[]))
    }

    postText() == "File type: text/plain;charset=US-ASCII"
  }

  @Unroll
  def "Error for #message"() {
    given:
    handlers {
      post(handler)
    }

    when:
    requestSpec { RequestSpec requestSpec ->
      requestSpec.headers.add("Content-Type", APPLICATION_FORM)
      requestSpec.body.text([a: "b"].collect({ it }).join('&'))
    }
    post()

    then:
    response.statusCode == 500

    where:
    [message, handler] << [["Put on Parsed Form",
                            new Handler() {
                              void handle(Context context) throws Exception {
                                def form = context.parse Form
                                form.put("bad", "idea")
                                context.render "I changed things I shoudn't"
                              }
                            }],
                           ["PutAll on Parsed Form", new Handler() {
                             void handle(Context context) throws Exception {
                               def form = context.parse Form
                               form.putAll([bad: "idea"])
                               context.render "I changed things I shoudn't"
                             }
                           }],
                           ["Remove on Parsed Form", new Handler() {
                             void handle(Context context) throws Exception {
                               def form = context.parse Form
                               form.remove("Bad")
                               context.render "I changed things I shoudn't"
                             }
                           }],
                           ["Clear on Parsed Form", new Handler() {
                             void handle(Context context) throws Exception {
                               def form = context.parse Form
                               form.clear()
                               context.render "I changed things I shoudn't"
                             }
                           }]]

  }


}
