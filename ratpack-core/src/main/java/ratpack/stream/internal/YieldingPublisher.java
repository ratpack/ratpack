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
import ratpack.func.Function;
import ratpack.stream.YieldRequest;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class YieldingPublisher<T> implements Publisher<T> {

  private final Function<? super YieldRequest, T> producer;
  private final AtomicLong subscriptionCounter = new AtomicLong();

  public YieldingPublisher(Function<? super YieldRequest, T> producer) {
    this.producer = producer;
  }

  @Override
  public void subscribe(final Subscriber<? super T> subscriber) {
    subscriber.onSubscribe(new Subscription() {
      private final long subscriptionNum = subscriptionCounter.getAndIncrement();
      private final AtomicInteger requestCounter = new AtomicInteger();
      private final AtomicLong waiting = new AtomicLong();
      private final AtomicBoolean done = new AtomicBoolean();
      private final AtomicBoolean draining = new AtomicBoolean();

      @Override
      public void request(long n) {
        waiting.addAndGet(n);
        drain();
      }

      @Override
      public void cancel() {
        done.set(true);
      }

      private void drain() {
        if (draining.compareAndSet(false, true)) {
          try {
            long waitingAmount = waiting.get();
            while (waitingAmount > 0) {
              T produced;

              try {
                produced = producer.apply(new DefaultYieldRequest(subscriptionNum, requestCounter.getAndIncrement()));
              } catch (Throwable e) {
                fireError(e);
                return;
              }

              if (produced == null) { // end of stream
                done.set(true);
                try {
                  subscriber.onComplete();
                } catch (Throwable e) {
                  e.printStackTrace();
                }
              }

              try {
                subscriber.onNext(produced);
              } catch (Throwable e) {
                fireError(e);
                return;
              }

              waitingAmount = waiting.decrementAndGet();
            }
          } finally {
            draining.set(false);
          }
        }
      }

      private void fireError(Throwable e) {
        done.set(true);
        subscriber.onError(e);
      }
    });
  }

  private static class DefaultYieldRequest implements YieldRequest {
    private final long requestNum;
    private final long subscriptionNum;

    public DefaultYieldRequest(long subscriptionNum, long requestNum) {
      this.subscriptionNum = subscriptionNum;
      this.requestNum = requestNum;
    }

    @Override
    public long getSubscriberNum() {
      return subscriptionNum;
    }

    @Override
    public long getRequestNum() {
      return requestNum;
    }
  }
}


