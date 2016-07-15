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

package ratpack.http

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.apache.commons.lang3.RandomStringUtils
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import ratpack.exec.Blocking
import ratpack.handling.Context
import ratpack.http.client.RequestSpec
import ratpack.registry.Registry
import ratpack.stream.Streams
import ratpack.test.internal.RatpackGroovyDslSpec

import java.nio.charset.StandardCharsets

class RequestBodyStreamReadingSpec extends RatpackGroovyDslSpec {

  def "can not read body"() {
    when:
    serverConfig {
      development true
    }
    handlers {
      post("ignore") {
        render "ok"
      }
      post("read") {
        render ResponseChunks.bufferChunks("text/plain", request.bodyStream)
      }
    }

    then:
    requestSpec { RequestSpec requestSpec -> requestSpec.body.stream({ it << "foo" }) }
    postText("ignore") == "ok"
    requestSpec { RequestSpec requestSpec -> requestSpec.body.stream({ it << "bar" }) }
    postText("read") == "bar"
  }

  def "can read body after redirect"() {
    when:
    serverConfig {
      development true
    }
    handlers {
      post("redirect") {
        redirect "read"
      }
      post("read") {
        render ResponseChunks.bufferChunks("text/plain", request.bodyStream)
      }
    }

    then:
    requestSpec { RequestSpec requestSpec -> requestSpec.body.stream({ it << "foo" }) }
    postText("redirect") == "foo"
  }

  def "can get request body in development"() {
    when:
    serverConfig {
      development true
    }
    handlers {
      post {
        render ResponseChunks.bufferChunks("text/plain", request.bodyStream)
      }
    }

    then:
    requestSpec { RequestSpec requestSpec -> requestSpec.body.stream({ it << "foo" }) }
    postText() == "foo"
  }

  def "can get request body async"() {
    when:
    handlers {
      post {
        Blocking.get { "foo " }.map { request.bodyStream }.then { body ->
          render ResponseChunks.bufferChunks("text/plain", body)
        }
      }
    }

    then:
    requestSpec { RequestSpec requestSpec -> requestSpec.body.stream({ it << "foo" }) }
    postText() == "foo"
  }

  def "can get request body as input stream"() {
    when:
    handlers {
      post {
        request.body.then { body ->
          response.send new String(body.inputStream.bytes, "utf8")
        }
      }
    }

    then:
    requestSpec { it.body.stream { it << "foo".getBytes("utf8") } }
    postText() == "foo"
  }

  def "can get large request body as bytes"() {
    given:
    def string = "a" * 1024 * 300

    when:
    handlers {
      post {
        render ResponseChunks.bufferChunks("text/plain", request.bodyStream)
      }
    }

    then:
    requestSpec { requestSpec ->
      requestSpec.body.stream({ it << string.getBytes("utf8") })
    }
    postText() == string
  }

  def "get bytes on get request"() {
    when:
    handlers {
      all {
        render ResponseChunks.bufferChunks("text/plain", request.bodyStream)
      }
    }

    then:
    getText() == ""
    postText() == ""
    putText() == ""
  }

  def "respect max content length"() {
    when:
    serverConfig {
      maxContentLength 16
    }
    handlers {
      post { ctx ->
        request.bodyStream.subscribe(new BufferThenSendSubscriber(context))
      }
    }

    then:
    requestSpec { RequestSpec requestSpec -> requestSpec.body.stream({ it << "bar".multiply(16) }) }
    def response = post()
    response.statusCode == 413
    requestSpec { RequestSpec requestSpec -> requestSpec.body.stream({ it << "foo" }) }
    postText() == "foo"
  }

  def "override acceptable max content length per request"() {
    when:
    serverConfig {
      maxContentLength 16
    }
    handlers {
      post {
        request.bodyStream.subscribe(new BufferThenSendSubscriber(context))
      }
      post("allow") {
        request.getBodyStream(16 * 8).subscribe(new BufferThenSendSubscriber(context))
      }
    }

    then:
    requestSpec { RequestSpec requestSpec -> requestSpec.body.stream({ it << "bar".multiply(16) }) }
    def response = post()
    response.statusCode == 413
    requestSpec { RequestSpec requestSpec -> requestSpec.body.stream({ it << "foo".multiply(16) }) }
    postText("allow") == "foo".multiply(16)
  }

  def "can read body only once"() {
    when:
    handlers {
      all {
        Streams.toList(request.bodyStream).map { Unpooled.unmodifiableBuffer(it as ByteBuf[]) }.then { body ->
          next(Registry.single(String, body.toString(StandardCharsets.UTF_8)))
        }
      }
      post {
        response.send get(String)
      }
      post("again") {
        request.body.then { body ->
          request.bodyStream.subscribe(new BufferThenSendSubscriber(context))
        }
      }
    }

    then:
    requestSpec { RequestSpec requestSpec -> requestSpec.body.stream({ it << "foo" }) }
    postText() == "foo"
    def response = post("again")
    response.statusCode == 500
  }

  def "can eagerly release body"() {
    when:
    handlers {
      post { ctx ->
        request.bodyStream.subscribe(new Subscriber<ByteBuf>() {
          @Override
          void onSubscribe(Subscription s) {
            s.request(Long.MAX_VALUE)
          }

          @Override
          void onNext(ByteBuf byteBuf) {
            byteBuf.release()
          }

          @Override
          void onError(Throwable t) {

          }

          @Override
          void onComplete() {
            ctx.render "ok"
          }
        })
      }
    }

    then:
    requestSpec {
      it.body.text("foo")
    }
    postText() == "ok"
  }

  def "can read large request body over same connection"() {
    given:
    def body = "a" * (4096 * 10)
    handlers {
      post {
        request.bodyStream.toList().then {
          it*.release()
          context.render(directChannelAccess.channel.id().asShortText())
        }
      }
    }

    when:
    HttpURLConnection connection = applicationUnderTest.address.toURL().openConnection()
    connection.setRequestMethod("POST")
    connection.doOutput = true
    connection.outputStream << body
    def channelId1 = connection.inputStream.text

    then:
    connection.getHeaderField("Connection") == "keep-alive"

    when:
    connection = applicationUnderTest.address.toURL().openConnection()
    connection.setRequestMethod("POST")
    connection.doOutput = true
    connection.outputStream << body
    def channelId2 = connection.inputStream.text

    then:
    connection.getHeaderField("Connection") == "keep-alive"

    and:
    channelId1 == channelId2
  }

  def "can stream large request body to disk"() {
    given:
    def string = RandomStringUtils.random(1024 * 1024 * 2)
    def file = baseDir.path("out.txt").toFile()
    file.createNewFile()

    when:
    handlers {
      post { ctx ->
        request.getBodyStream(string.bytes.length).subscribe(new Subscriber<ByteBuf>() {
          OutputStream out
          Subscription subscription

          @Override
          void onSubscribe(Subscription s) {
            subscription = s
            out = file.newOutputStream()
            s.request(1)
          }

          @Override
          void onNext(ByteBuf byteBuf) {
            Blocking.op {
              try {
                byteBuf.getBytes(0, out, byteBuf.readableBytes())
              } finally {
                byteBuf.release()
              }
            } then {
              subscription.request 1
            }
          }

          @Override
          void onError(Throwable t) {
            out.close()
            ctx.error(t)
          }

          @Override
          void onComplete() {
            out.close()
            ctx.render "ok"
          }
        })
      }
    }

    then:
    requestSpec { it.body.text string }
    postText() == "ok"
    file.text == string
  }

  class BufferThenSendSubscriber implements Subscriber<ByteBuf> {

    private final List<ByteBuf> byteBufs = []
    private final Context context

    BufferThenSendSubscriber(Context context) {
      this.context = context
    }

    @Override
    void onSubscribe(Subscription s) {
      s.request(Long.MAX_VALUE)
    }

    @Override
    void onNext(ByteBuf byteBuf) {
      byteBufs << byteBuf
    }

    @Override
    void onError(Throwable t) {
      byteBufs*.release()
      if (t instanceof RequestBodyTooLargeException) {
        context.clientError(413)
      } else {
        context.error(t)
      }
    }

    @Override
    void onComplete() {
      context.response.send(Unpooled.unmodifiableBuffer(byteBufs as ByteBuf[]))
    }
  }
}
