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

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class SimpleNonConcurrentPushPublisher<T> implements PushPublisher<T> {

  private Subscriber<? super T> subscriber;
  private boolean cancelled;

  private final PushStream<T> pushStream = new PushStream<T>() {
    @Override
    public void push(T item) {
      if (subscriber != null && !cancelled) {
        subscriber.onNext(item);
      }
    }

    @Override
    public void complete() {
      if (subscriber != null && !cancelled) {
        subscriber.onComplete();
      }
    }

    @Override
    public void error(Throwable throwable) {
      if (subscriber != null && !cancelled) {
        subscriber.onComplete();
      }
    }
  };

  @Override
  public void subscribe(Subscriber<? super T> subscriber) {
    if (this.subscriber == null) {
      this.subscriber = subscriber;
      subscriber.onSubscribe(new Subscription() {
        @Override
        public void request(long n) {
          // ignore, we expect to be buffered
        }

        @Override
        public void cancel() {
          cancelled = true;
        }
      });

    } else {
      subscriber.onError(new IllegalStateException("publisher is single use"));
    }
  }

  @Override
  public PushStream<T> getStream() {
    return pushStream;
  }

}
