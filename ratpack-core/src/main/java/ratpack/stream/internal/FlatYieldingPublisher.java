/*
 * Copyright 2015 the original author or authors.
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
import ratpack.exec.Promise;
import ratpack.func.Function;
import ratpack.stream.TransformablePublisher;
import ratpack.stream.YieldRequest;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FlatYieldingPublisher<T> implements TransformablePublisher<T> {

  private final Function<? super YieldRequest, ? extends Promise<? extends T>> producer;
  private final AtomicLong subscriptionCounter = new AtomicLong();

  public FlatYieldingPublisher(Function<? super YieldRequest, ? extends Promise<? extends T>> producer) {
    this.producer = producer;
  }

  @Override
  public void subscribe(final Subscriber<? super T> subscriber) {
    new Subscription(subscriber);
  }

  private class Subscription extends SubscriptionSupport<T> {

    private final long subscriptionNum = subscriptionCounter.getAndIncrement();
    private final AtomicInteger requestCounter = new AtomicInteger();
    private final AtomicLong waiting = new AtomicLong();
    private final AtomicBoolean draining = new AtomicBoolean();

    public Subscription(Subscriber<? super T> subscriber) {
      super(subscriber);
      start();
    }

    @Override
    protected void doRequest(long n) {
      waiting.addAndGet(n);
      drain();
    }

    private void drain() {
      if (!isStopped() && draining.compareAndSet(false, true)) {
        long l = waiting.getAndDecrement();
        if (l > 0) {
          try {
            Promise<? extends T> promise = producer.apply(new DefaultYieldRequest(subscriptionNum, requestCounter.getAndIncrement()));
            promise
              .wiretap(r ->
                this.draining.set(false)
              )
              .onError(this::onError)
              .then(t -> {
                if (t == null) {
                  onComplete();
                } else {
                  onNext(t);
                  drain();
                }
              });
          } catch (Throwable e) {
            draining.set(false);
            onError(e);
            return;
          }
        } else {
          waiting.addAndGet(1);
          draining.set(false);
        }
        if (waiting.get() > 0) {
          drain();
        }
      }
    }

  }

}


