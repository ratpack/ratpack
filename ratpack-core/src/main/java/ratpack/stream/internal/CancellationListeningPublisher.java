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

package ratpack.stream.internal;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.func.Action;

public class CancellationListeningPublisher<T> implements Publisher<T> {

  private final static Logger LOGGER = LoggerFactory.getLogger(CancellationListeningPublisher.class);

  private final Publisher<T> publisher;
  private final Action<Void> listener;

  public CancellationListeningPublisher(Publisher<T> publisher, Action<Void> listener) {
    this.publisher = publisher;
    this.listener = listener;
  }

  @Override
  public void subscribe(final Subscriber<T> outSubscriber) {
    publisher.subscribe(new Subscriber<T>() {

      @Override
      public void onSubscribe(final Subscription subscription) {
        outSubscriber.onSubscribe(new Subscription() {
          @Override
          public void request(int n) {
            subscription.request(n);
          }

          @Override
          public void cancel() {
            try {
              listener.execute(null);
            } catch (Throwable e) {
              LOGGER.warn("ignoring exception thrown by cancel listener", e);
            } finally {
              subscription.cancel();
            }
          }
        });
      }

      @Override
      public void onNext(T in) {
        outSubscriber.onNext(in);
      }

      @Override
      public void onError(Throwable t) {
        outSubscriber.onError(t);
      }

      @Override
      public void onComplete() {
        outSubscriber.onComplete();
      }
    });
  }
}
