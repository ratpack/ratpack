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

package ratpack.codahale.metrics.internal;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import ratpack.websocket.WebSocket;

public class WebsocketBroadcastSubscriber implements Subscriber<String>, AutoCloseable {
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
    if (this.subscription == null) {
      this.subscription = s;
      this.subscription.request(Integer.MAX_VALUE);
    } else {
      this.subscription.cancel();
    }
  }

  @Override
  public void onNext(String s) {
    if (!terminated) {
      webSocket.send(s);
    }
  }

  @Override
  public void onError(Throwable t) {
    if (!terminated) {
      webSocket.close();
    }
  }

  @Override
  public void onComplete() {
    if (!terminated) {
      webSocket.close();
    }
  }
}
