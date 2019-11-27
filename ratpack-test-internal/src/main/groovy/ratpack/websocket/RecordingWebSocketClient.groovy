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
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.java_websocket.client.WebSocketClient
import org.java_websocket.framing.Framedata
import org.java_websocket.handshake.ServerHandshake

import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@CompileStatic
class RecordingWebSocketClient extends WebSocketClient {

  final LinkedBlockingQueue<String> receivedText = new LinkedBlockingQueue<String>()
  final LinkedBlockingQueue<ByteBuf> receivedBytes = new LinkedBlockingQueue<ByteBuf>()
  final LinkedBlockingQueue<Framedata> receivedFragments = new LinkedBlockingQueue<Framedata>()

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

  @SuppressWarnings("GrDeprecatedAPIUsage")
  @Override
  void onFragment(Framedata frame) {
    receivedFragments.put frame
  }

  @Override
  void onWebsocketPong(org.java_websocket.WebSocket conn, Framedata f) {
    receivedFragments.put f
  }

  @Override
  void onMessage(String message) {
    receivedText.put message
  }

  @Override
  void onMessage(ByteBuffer byteBuffer) {
    receivedBytes.put Unpooled.wrappedBuffer(byteBuffer)
  }

  @Override
  void onClose(int code, String reason, boolean remote) {
    this.closeCode = code
    this.closeReason = reason
    this.closeRemote = remote
    closeLatch.countDown()
  }

  void waitForClose() {
    assert closeLatch.await(5, TimeUnit.SECONDS): "websocket connection did not close"
  }

  @Override
  void onError(Exception ex) {
    this.exception = ex
  }

}
