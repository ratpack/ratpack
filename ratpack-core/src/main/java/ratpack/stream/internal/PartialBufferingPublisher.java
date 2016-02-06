/*
 * Copyright 2016 the original author or authors.
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.stream.TransformablePublisher;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class PartialBufferingPublisher<T> implements TransformablePublisher<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PartialBufferingPublisher.class);

  private final Action<? super T> disposer;
  private final Function<? super BufferedWriteStream<T>, Subscription> function;

  public PartialBufferingPublisher(Action<? super T> disposer, Function<? super BufferedWriteStream<T>, Subscription> function) {
    this.disposer = disposer;
    this.function = function;
  }

  @Override
  public void subscribe(final Subscriber<? super T> subscriber) {
    new Buffer(subscriber);
  }

  private class Buffer extends SubscriptionSupport<T> {

    private final AtomicBoolean upstreamFinished = new AtomicBoolean();
    private final org.reactivestreams.Subscription upstreamSubscription;

    private final AtomicLong wanted = new AtomicLong(Long.MIN_VALUE);
    private final AtomicBoolean open = new AtomicBoolean();
    private final ConcurrentLinkedQueue<T> buffer = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean draining = new AtomicBoolean();
    private final AtomicBoolean disposing = new AtomicBoolean();

    public Buffer(Subscriber<? super T> subscriber) {
      super(subscriber);
      org.reactivestreams.Subscription upstreamSubscriptionTmp = null;
      try {
        upstreamSubscriptionTmp = function.apply(new BufferedWriteStream<T>() {
          @Override
          public void item(T item) {
            buffer.add(item);
            tryDrain();
          }

          @Override
          public void error(Throwable throwable) {
            disposing.set(true);
            open.set(true);
            tryDrain();
            Buffer.this.onError(throwable);
          }

          @Override
          public void complete() {
            upstreamFinished.set(true);
            tryDrain();
          }

          @Override
          public long getRequested() {
            return wanted.get() + Long.MAX_VALUE;
          }

          @Override
          public long getBuffered() {
            return buffer.size();
          }
        });
        start();
      } catch (Exception e) {
        subscriber.onError(e);
      }
      this.upstreamSubscription = upstreamSubscriptionTmp;
    }

    private void tryDrain() {
      if (draining.compareAndSet(false, true)) {
        try {
          long i = wanted.get();
          while (!isStopped() && (open.get() || i > Long.MIN_VALUE)) {
            T item = buffer.poll();
            if (item == null) {
              if (upstreamFinished.get()) {
                Buffer.this.onComplete();
                return;
              } else {
                break;
              }
            } else {
              if (disposing.get()) {
                try {
                  disposer.execute(item);
                } catch (Exception e) {
                  LOGGER.warn("exception raised disposing of " + item + " - will be ignored", e);
                }
              } else {
                Buffer.this.onNext(item);
              }
              i = i >= 0 ? 0 : wanted.decrementAndGet();
            }
          }
        } finally {
          draining.set(false);
        }
        if (buffer.peek() != null && wanted.get() > Long.MIN_VALUE) {
          tryDrain();
        }
      }
    }

    protected void doRequest(long n) {
      if (wanted.get() < 0) {
        long nowWanted = wanted.addAndGet(n);
        if (nowWanted >= 0) {
          upstreamSubscription.request(Long.MAX_VALUE);
          open.set(true);
        } else {
          long outstanding = nowWanted + Long.MAX_VALUE + 1 - buffer.size();
          if (outstanding > 0) {
            upstreamSubscription.request(n);
          }
        }
      }
      tryDrain();
    }

    @Override
    protected void doCancel() {
      disposing.set(true);
      open.set(true);
      upstreamSubscription.cancel();
      tryDrain();
    }

  }
}
