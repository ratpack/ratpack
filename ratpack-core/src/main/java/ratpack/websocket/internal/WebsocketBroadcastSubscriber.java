/*
 * Copyright 2014 the original author or authors.
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

package ratpack.websocket.internal;

import io.netty.buffer.ByteBuf;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import ratpack.websocket.WebSocket;

public class WebsocketBroadcastSubscriber implements Subscriber<ByteBuf>, AutoCloseable {
  private final WebSocket webSocket;
  private Subscription subscription;
  protected boolean terminated;

  public WebsocketBroadcastSubscriber(WebSocket webSocket) {
    this.webSocket = webSocket;
  }

  @Override
  public void close() {
    terminated = true;
    if (subscription != null) {
      this.subscription.cancel();
    }
  }

  @Override
  public void onSubscribe(Subscription s) {
    if (this.subscription != null) {
      s.cancel();
      return;
    }

    this.subscription = s;
    this.subscription.request(1);
  }

  @Override
  public void onNext(ByteBuf s) {
    if (s == null) {
      throw new NullPointerException();
    }

    if (!terminated) {
      webSocket.send(s);
      this.subscription.request(1);
    } else {
      s.release();
    }
  }

  @Override
  public void onError(Throwable t) {
    if (!terminated) {
      webSocket.close(1011, t.getMessage());
    }
  }

  @Override
  public void onComplete() {
    if (!terminated) {
      webSocket.close();
    }
  }
}
