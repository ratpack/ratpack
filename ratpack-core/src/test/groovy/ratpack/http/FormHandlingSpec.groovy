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
import io.netty.buffer.ByteBufAllocator
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

import static ratpack.form.Form.form
import static ratpack.http.MediaType.APPLICATION_FORM

class FormHandlingSpec extends RatpackGroovyDslSpec {

  def setup() {
    bindings {
      bindInstance ServerErrorHandler, new DefaultDevelopmentErrorHandler()
    }
  }

  def "can get form params"() {
    given:
    handlers {
      post {
        def form = parse Form
        form.then {
          render it.toString()
        }
      }
    }

    when:
    requestSpec { it.headers.add("Content-Type", APPLICATION_FORM) }
    then:
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
    given:
    handlers {
      post {
        def form = parse Form
        form.then {
          render it.toString()
        }
      }
    }

    when:
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
        chunks << httpPostRequestEncoder.readChunk(null as ByteBufAllocator).content()
      }
      requestSpec.body.buffer(Unpooled.wrappedBuffer(chunks as ByteBuf[]))
    }

    then:
    postText() == "[foo:[1], bar:[2, 3]]"
  }

  def "can handle file uploads"() {
    given:
    def fooFile = write "foo.txt", "bar"
    handlers {
      post {
        def form = parse Form
        form.then {
          render "File content: " + it.file("theFile").text
        }
      }
    }

    when:
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
        chunks << httpPostRequestEncoder.readChunk(null as ByteBufAllocator).content()
      }
      requestSpec.body.buffer(Unpooled.wrappedBuffer(chunks as ByteBuf[]))
    }
    then:
    postText() == "File content: bar"
  }

  def "default encoding is utf-8"() {
    given:
    def fooFile = write "foo.txt", "bar"
    handlers {
      post {
        def form = parse Form
        form.then {
          render "File type: " + it.file("theFile").contentType
        }
      }
    }

    when:
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
        chunks << httpPostRequestEncoder.readChunk(null as ByteBufAllocator).content()
      }
      requestSpec.body.buffer(Unpooled.wrappedBuffer(chunks as ByteBuf[]))
    }
    then:
    postText() == "File type: text/plain; charset=utf-8"
  }

  def "respects custom encoding"() {
    given:
    def fooFile = write "foo.txt", "bar"
    handlers {
      post {
        def form = parse Form
        form.then {
          render "File type: " + it.file("theFile").contentType
        }
      }
    }

    when:
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
        chunks << httpPostRequestEncoder.readChunk(null as ByteBufAllocator).content()
      }
      requestSpec.body.buffer(Unpooled.wrappedBuffer(chunks as ByteBuf[]))
    }
    then:
    postText() == "File type: text/plain; charset=us-ascii"
  }

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
                                form.then {
                                  it.put("bad", "idea")
                                  context.render "I changed things I shouldn't"
                                }
                              }
                            }],
                           ["PutAll on Parsed Form", new Handler() {
                             void handle(Context context) throws Exception {
                               def form = context.parse Form
                               form.then {
                                 it.putAll([bad: "idea"])
                                 context.render "I changed things I shouldn't"
                               }
                             }
                           }],
                           ["Remove on Parsed Form", new Handler() {
                             void handle(Context context) throws Exception {
                               def form = context.parse Form
                               form.then {
                                 it.remove("Bad")
                                 context.render "I changed things I shouldn't"
                               }
                             }
                           }],
                           ["Clear on Parsed Form", new Handler() {
                             void handle(Context context) throws Exception {
                               def form = context.parse Form
                               form.then {
                                 it.clear()
                                 context.render "I changed things I shouldn't"
                               }
                             }
                           }]]
  }

  def "can parse form from only query parameters"() {
    given:
    handlers {
      post {
        def form = parse form(true)
        form.then {
          render it.toString()
        }
      }
    }

    when:
    requestSpec { it.headers.add("Content-Type", APPLICATION_FORM) }
    then:
    postText() == "[:]"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec ->
      requestSpec.headers.add("Content-Type", APPLICATION_FORM)
    }
    then:
    postText("?a=b") == "[a:[b]]"

    when:
    resetRequest()
    requestSpec { RequestSpec requestSpec ->
      requestSpec.headers.add("Content-Type", APPLICATION_FORM)
    }
    then:
    postText("?a=b&a=c&d=e&abc=") == "[a:[b, c], d:[e], abc:[]]"
  }

  def "request body merged with query parameters"() {
    given:
    handlers {
      post {
        def form = parse form(true)
        form.then {
          render it.toString()
        }
      }
    }

    when: "different keys in body and params"
    requestSpec { RequestSpec requestSpec ->
      def body = "d=e"
      requestSpec.headers.add("Content-Type", APPLICATION_FORM)
      requestSpec.body.stream({ it << body })
    }
    then:
    postText("?a=b") == "[a:[b], d:[e]]"

    when: "same keys in body and params"
    resetRequest()
    requestSpec { RequestSpec requestSpec ->
      def body = "a=c"
      requestSpec.headers.add("Content-Type", APPLICATION_FORM)
      requestSpec.body.stream({ it << body })
    }
    then:
    postText("?a=b") == "[a:[b, c]]"
  }

}
