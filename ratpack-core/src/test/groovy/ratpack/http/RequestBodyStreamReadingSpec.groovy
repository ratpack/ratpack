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
import io.netty.buffer.PooledByteBufAllocator
import io.netty.buffer.Unpooled
import org.apache.commons.lang3.RandomStringUtils
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import ratpack.exec.Blocking
import ratpack.exec.Promise
import ratpack.http.client.RequestSpec
import ratpack.registry.Registry
import ratpack.stream.Streams
import ratpack.stream.bytebuf.ByteBufStreams
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.util.concurrent.BlockingVariable

import java.nio.charset.Charset
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
        render(composeString(request.bodyStream))
      }
    }

    then:
    requestSpec { RequestSpec requestSpec -> requestSpec.body.stream({ it << "foo" }) }
    postText("ignore") == "ok"
    def bar = "bar" * (1024 * 8)
    requestSpec { RequestSpec requestSpec -> requestSpec.body.stream({ it << bar }) }
    postText("read") == bar
  }

  def "can read body after redirect"() {
    when:
    serverConfig {
      development true
    }
    handlers {
      post("redirect") {
        redirect 303, "read"
      }
      post("read") {
        render(composeString(request.bodyStream))
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
        render(composeString(request.bodyStream))
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
        Blocking.get { "foo " }.map { composeString(request.bodyStream) }.then { body ->
          render body
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
        render composeString(request.bodyStream)
      }
    }

    then:
    requestSpec { RequestSpec requestSpec -> requestSpec.body.stream({ it << "bar" * 100000 }) }
    def response = post()
    response.statusCode == 413
  }

  def "override acceptable max content length per request"() {
    when:
    serverConfig {
      maxContentLength 16
    }
    handlers {
      post {
        render composeString(request.bodyStream)
      }
      post("allow") {
        render composeString(request.getBodyStream(16 * 8))
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
          def string = body.toString(StandardCharsets.UTF_8)
          body.release()
          next(Registry.single(String, string))
        }
      }
      post {
        response.send get(String)
      }
      post("again") {
        request.body.then { body ->
          composeString(request.bodyStream)
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
    connection.getHeaderField("Connection") == null

    when:
    connection = applicationUnderTest.address.toURL().openConnection()
    connection.setRequestMethod("POST")
    connection.doOutput = true
    connection.outputStream << body
    def channelId2 = connection.inputStream.text

    then:
    connection.getHeaderField("Connection") == null

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

  def "request body stream errors when client closes connection unexpectedly"() {
    when:
    def error = new BlockingVariable<Throwable>()
    handlers {
      all { ctx ->
        ByteBufStreams.compose(request.bodyStream, PooledByteBufAllocator.DEFAULT)
          .onError { error.set(it); response.send() }
          .then { response.send(it) }
      }
    }

    and:
    withSocket {
      write("POST / HTTP/1.1\r\n")
      write("Content-Length: 1024000\r\n")
      write("\r\n")
      write("${'a' * 1024}\r\n")
      close()
    }

    then:
    error.get() instanceof ConnectionClosedException
  }

  Promise<String> composeString(Publisher<? extends ByteBuf> publisher) {
    ByteBufStreams.compose(publisher).map {
      def s = it.toString(Charset.defaultCharset())
      it.release()
      s
    }
  }

}
