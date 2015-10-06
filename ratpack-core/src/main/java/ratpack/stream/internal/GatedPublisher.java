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
import ratpack.func.Action;
import ratpack.stream.TransformablePublisher;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class GatedPublisher<T> implements TransformablePublisher<T> {

  private final Publisher<? extends T> upstream;
  private final Action<? super Runnable> releaseReceiver;

  public GatedPublisher(Publisher<? extends T> upstream, Action<? super Runnable> releaseReceiver) {
    this.upstream = upstream;
    this.releaseReceiver = releaseReceiver;
  }

  @Override
  public void subscribe(final Subscriber<? super T> downstream) {
    final GatedSubscriber<? super T> gatedSubscriber = new GatedSubscriber<>(downstream);
    try {
      releaseReceiver.execute((Runnable) gatedSubscriber::open);
    } catch (final Throwable e) {
      downstream.onSubscribe(new Subscription() {
        @Override
        public void request(long n) {
          downstream.onError(e);
        }

        @Override
        public void cancel() {

        }
      });
    }
    upstream.subscribe(gatedSubscriber);
  }

  private static class GatedSubscriber<T> implements Subscriber<T> {

    private Subscription upstreamSubscription;

    private final Subscriber<T> downstream;
    private final AtomicBoolean open = new AtomicBoolean();
    private final AtomicBoolean done = new AtomicBoolean();
    private final AtomicBoolean draining = new AtomicBoolean();
    private final AtomicLong waiting = new AtomicLong();

    public GatedSubscriber(Subscriber<T> downstream) {
      this.downstream = downstream;
    }

    private void open() {
      open.set(true);
      drain();
    }

    private void drain() {
      if (draining.compareAndSet(false, true)) {
        try {
          if (open.get()) {
            long requested = waiting.getAndSet(0);
            if (requested > 0) {
              upstreamSubscription.request(requested);
            }
          }
        } finally {
          draining.set(false);
        }
      }

      if (open.get() && waiting.get() > 0) {
        drain();
      }
    }

    @Override
    public void onSubscribe(Subscription s) {
      upstreamSubscription = s;
      downstream.onSubscribe(new DownstreamSubscription());
    }

    @Override
    public void onNext(T t) {
      downstream.onNext(t);
    }

    @Override
    public void onError(Throwable t) {
      downstream.onError(t);
    }

    @Override
    public void onComplete() {
      downstream.onComplete();
    }

    private class DownstreamSubscription implements Subscription {
      @Override
      public void request(long n) {
        if (open.get() && !done.get()) {
          upstreamSubscription.request(n);
        } else {
          waiting.addAndGet(n);
        }
      }

      @Override
      public void cancel() {
        upstreamSubscription.cancel();
      }
    }
  }
}
