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

package ratpack.http.client

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import ratpack.exec.Execution
import ratpack.stream.Streams

import java.time.Duration
import java.util.concurrent.CountDownLatch

class HttpClientKeepAliveSpec extends BaseHttpClientSpec {

  def poolingHttpClient = HttpClient.of { it.poolSize(1) }

  def "connection can be reused"() {
    when:
    otherApp {
      get {
        render "ok"
      }
    }
    handlers {
      get {
        render poolingHttpClient.get(otherAppUrl()).map { it.body.text }
      }
    }

    then:
    text == "ok"
    text == "ok"
  }

  def "keep alive connection is silently discarded when it closes"() {
    when:
    Channel channel = null
    def latch = new CountDownLatch(1)
    otherApp {
      get {
        channel = context.directChannelAccess.channel
        channel.closeFuture().addListener {
          latch.countDown()
        }
        render "ok"
      }
    }
    handlers {
      get {
        render poolingHttpClient.get(otherAppUrl()).map { it.body.text }
      }
    }

    then:
    text == "ok"
    latch.count == 1

    when:
    channel.close().sync()

    then:
    latch.await()
    text == "ok"
  }

  def "clients can safely be used across different exec controllers"() {
    when:
    Channel channel
    handlers { get { render poolingHttpClient.get(otherAppUrl()).map { it.body.text } } }
    otherApp {
      get {
        channel = directChannelAccess.channel
        render "ok"
      }
    }

    then:
    text == "ok"
    channel.isOpen()

    when:
    application.close()
    channel.closeFuture().get()

    then:
    text == "ok"
  }

  def "connection is removed from pool if server closes the connection"() {
    when:
    Channel channel1
    Channel channel2
    otherApp {
      get {
        Execution.fork().start {
          it.sleep(Duration.ofMillis(500)).then {
            def channel = context.directChannelAccess.channel
            if (channel1 == null) {
              channel1 = channel
            } else {
              channel2 = channel
            }
            channel.close()
          }
        }
        header("content-length", 1024000)
        response.sendStream Streams.periodically(context, Duration.ofMillis(100), { Unpooled.wrappedBuffer("a".bytes) })
      }
    }
    handlers {
      def http = HttpClient.of {
        it.poolSize(1)
      }
      get {
        render http.get(otherAppUrl()).map { it.body.text }
      }
    }

    then:
    get().statusCode == 500
    get().statusCode == 500

    and:
    channel1.id().asShortText() != channel2.id().asShortText()
  }

}
