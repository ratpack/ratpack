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

import groovy.transform.CompileStatic
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake

import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@CompileStatic
class RecordingWebSocketClient extends WebSocketClient {

  final LinkedBlockingQueue<String> received = new LinkedBlockingQueue<String>()
  Exception exception
  int closeCode
  String closeReason
  boolean closeRemote
  ServerHandshake serverHandshake

  private final CountDownLatch closeLatch = new CountDownLatch(1)

  RecordingWebSocketClient(URI serverURI) {
    super(serverURI)
  }

  @Override
  void onOpen(ServerHandshake handshakedata) {
    serverHandshake = handshakedata
  }

  @Override
  void onMessage(String message) {
    received.put message
  }

  @Override
  void onClose(int code, String reason, boolean remote) {
    this.closeCode = code
    this.closeReason = reason
    this.closeRemote = remote
    closeLatch.countDown()
  }

  void waitForClose() {
    assert closeLatch.await(5, TimeUnit.SECONDS) : "websocket connection did not close"
  }

  @Override
  void onError(Exception ex) {
    this.exception = ex
  }

}
