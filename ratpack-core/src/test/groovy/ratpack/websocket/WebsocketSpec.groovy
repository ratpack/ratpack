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

import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.util.concurrent.BlockingVariable

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

import static ratpack.websocket.WebSockets.websocket

class WebsocketSpec extends RatpackGroovyDslSpec {

  def "can use websockets"() {
    when:
    def closing = new BlockingVariable<WebSocketClose>()
    def serverReceived = new LinkedBlockingQueue<WebSocketFrame>()
    def ws

    app {
      handlers {
        get {
          websocket(context).onClose { WebSocketClose close ->
            closing.set(close)
          } onMessage { WebSocketFrame frame ->
            serverReceived.put frame
            frame.connection.send(frame.text.toUpperCase())
          } connect {
            ws = it
          }
        }
      }
    }

    and:
    startServerIfNeeded()
    def client = new WebSocketClient(new URI("ws://localhost:$server.bindPort")) {
      def received = new LinkedBlockingQueue<String>()

      @Override
      void onOpen(ServerHandshake handshakedata) {

      }

      @Override
      void onMessage(String message) {
        received.put(message)
      }

      @Override
      void onClose(int code, String reason, boolean remote) {

      }

      @Override
      void onError(Exception ex) {

      }
    }

    then:
    client.connectBlocking()
    client.send("foo")

    and:
    serverReceived.poll(5, TimeUnit.SECONDS).text == "foo"
    client.received.poll(5, TimeUnit.SECONDS) == "FOO"

    when:
    client.closeBlocking()

    then:
    closing.get().fromClient

    cleanup:
    client.closeBlocking()
  }

}
