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

package ratpack.websocket

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import ratpack.exec.ExecController
import ratpack.func.Function
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.util.concurrent.BlockingVariable

import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

import static ratpack.stream.Streams.periodically
import static ratpack.stream.Streams.publish
import static ratpack.websocket.WebSockets.websocket
import static ratpack.websocket.WebSockets.websocketBroadcast

class WebSocketTestSpec extends RatpackGroovyDslSpec {

  def "can send and receive websockets"() {
    when:
    def closing = new BlockingVariable<WebSocketClose<Integer>>()
    def serverReceived = new LinkedBlockingQueue<WebSocketMessage<Integer>>()
    WebSocket ws

    handlers {
      get {
        context.websocket({
          ws = it
          2
        } as Function) connect {
          it.onClose {
            closing.set(it)
          } onMessage {
            serverReceived.put it
            it.connection.send(it.text.toUpperCase())
          }
        }
      }
    }

    and:
    server.start()
    def client = openWsClient()

    then:
    client.connectBlocking()
    client.send("foo")

    and:
    with(serverReceived.poll(5, TimeUnit.SECONDS)) {
      text == "foo"
      openResult == 2
    }
    client.received.poll(5, TimeUnit.SECONDS) == "FOO"

    when:
    client.closeBlocking()

    then:
    with(closing.get()) {
      fromClient
      openResult == 2
    }

    //noinspection GroovyVariableNotAssigned
    !ws.open

    cleanup:
    client?.closeBlocking()
  }

  def RecordingWebSocketClient openWsClient() {
    new RecordingWebSocketClient(new URI("ws://localhost:$server.bindPort"))
  }

  def "client receives error when exception thrown during server open"() {
    when:
    handlers {
      get {
        websocket(context) {
          throw new Exception("!")
        }.connect {}
      }
    }
    server.start()

    and:
    def client = openWsClient()
    client.connectBlocking()

    then:
    client.waitForClose()
    client.closeCode == 1011

    cleanup:
    client?.closeBlocking()

  }

  def "can broadcast over websockets and publishing stops after client closes"() {
    when:
    def streamCancelled = new CountDownLatch(1)

    handlers {
      get { ctx ->
        def stream = periodically(ctx.get(ExecController).executor, Duration.ofMillis(100)) {
          "1"
        }.wiretap {
          if (it.cancel) {
            streamCancelled.countDown()
          }
        }
        websocketBroadcast(context, stream)
      }
    }

    and:
    server.start()
    def client = openWsClient()

    then:
    client.connectBlocking()
    client.received.poll(5, TimeUnit.SECONDS) == "1"

    when:
    client.closeBlocking()

    then:
    streamCancelled.await()
    !client.closeRemote

    cleanup:
    client?.closeBlocking()
  }

  def "can broadcast over websockets and stream completes"() {
    when:
    handlers {
      get {
        def stream = publish(0..2).map { "foo-$it".toString() }
        websocketBroadcast(context, stream)
      }
    }

    and:
    server.start()
    def client = openWsClient()
    client.connectBlocking()

    then:
    client.received.poll(5, TimeUnit.SECONDS) == "foo-0"
    client.received.poll(5, TimeUnit.SECONDS) == "foo-1"
    client.received.poll(5, TimeUnit.SECONDS) == "foo-2"

    then:
    client.waitForClose()

    and:
    client.closeRemote == true
    client.closeCode == 1000

    cleanup:
    client?.closeBlocking()
  }

  def "websocket closes correctly when a streaming error occurs"() {
    when:
    handlers {
      get {
        def stream = publish(0..4).map {
          if (it < 3) {
            "foo-$it".toString()
          } else {
            throw new Exception("foo error")
          }
        }
        websocketBroadcast(context, stream)
      }
    }

    and:
    server.start()
    def client = openWsClient()
    client.connectBlocking()

    then:
    client.received.poll(5, TimeUnit.SECONDS) == "foo-0"
    client.received.poll(5, TimeUnit.SECONDS) == "foo-1"
    client.received.poll(5, TimeUnit.SECONDS) == "foo-2"

    then:
    client.waitForClose()

    and:
    client.closeRemote == true
    client.closeCode == 1011
    client.closeReason == "foo error"

    cleanup:
    client?.closeBlocking()
  }

  def "broadcast publisher does not need to synchronously subscribe"() {
    when:
    handlers {
      get {
        def executor = context.execution.controller.executor
        websocketBroadcast(context, new Publisher<String>() {
          @Override
          void subscribe(Subscriber<? super String> s) {
            def publisher = periodically(executor, Duration.ofSeconds(1)) {
              it < 1 ? "foo" : null
            }
            Thread.start { publisher.subscribe(s) }
          }
        })
      }
    }

    and:
    server.start()
    def client = openWsClient()

    then:
    client.connectBlocking()

    and:
    client.received.poll(5, TimeUnit.SECONDS) == "foo"

    cleanup:
    client?.closeBlocking()
  }

  def "onClose method is called when socket is closed abruptly"() {
    setup:
    def closed = new BlockingVariable<Boolean>(2)
    def connected = new BlockingVariable<Boolean>(2)

    handlers {
      get {
        context.websocket {
          connected.set(true)
          null
        } connect {
          it.onClose { closed.set(true) }
        }
      }
    }

    server.start()

    when:
    def client = openWsClient()
    client.connectBlocking()

    then:
    connected.get()

    when:
    client.connection.eot()

    then:
    closed.get()
  }

}
