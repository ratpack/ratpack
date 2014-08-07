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

package ratpack.http.internal

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import ratpack.http.ServerSentEvent

class SseStreamer implements Publisher<ServerSentEvent> {
  @Override
  void subscribe(Subscriber<ServerSentEvent> subscriber) {
    Subscription subscription = new Subscription() {

      @Override
      void cancel() { }

      @Override
      void request(int elements) {
        Thread.start {
          (1..3).each {
            subscriber.onNext(new ServerSentEvent(it.toString(), "add", "Event $it".toString()))
          }

          subscriber.onComplete()
        }
      }
    }

    subscriber.onSubscribe(subscription)
  }
}
