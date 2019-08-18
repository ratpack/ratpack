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
import ratpack.func.Function;
import ratpack.stream.TransformablePublisher;
import ratpack.stream.YieldRequest;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class YieldingPublisher<T> implements TransformablePublisher<T> {

  private final Function<? super YieldRequest, ? extends T> producer;
  private final AtomicLong subscriptionCounter = new AtomicLong();

  public YieldingPublisher(Function<? super YieldRequest, ? extends T> producer) {
    this.producer = producer;
  }

  @Override
  public void subscribe(final Subscriber<? super T> subscriber) {
    new Subscription(subscriber);
  }

  private class Subscription extends SubscriptionSupport<T> {

    private final long subscriptionNum = subscriptionCounter.getAndIncrement();
    private final AtomicInteger requestCounter = new AtomicInteger();

    public Subscription(Subscriber<? super T> subscriber) {
      super(subscriber);
      start();
    }

    @Override
    protected void doRequest(long n) {
      long i = 0;

      while (i++ < n) {
        if (isStopped()) {
          return;
        }

        T produced;
        try {
          produced = producer.apply(new DefaultYieldRequest(subscriptionNum, requestCounter.getAndIncrement()));
        } catch (Throwable e) {
          onError(e);
          return;
        }

        if (produced == null) { // end of stream
          onComplete();
          return;
        } else {
          onNext(produced);
        }
      }
    }
  }

}


