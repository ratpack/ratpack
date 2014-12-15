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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

abstract class SubscriptionSupport<T> implements Subscription {

  private final Subscriber<? super T> subscriber;

  private final AtomicBoolean started = new AtomicBoolean();
  private final AtomicBoolean stopped = new AtomicBoolean();

  private final AtomicLong waitingRequests = new AtomicLong();
  private final AtomicBoolean drainingRequests = new AtomicBoolean();

  private final AtomicBoolean complete = new AtomicBoolean();
  private final AtomicReference<Throwable> error = new AtomicReference<>();

  private final AtomicBoolean inOnMethod = new AtomicBoolean();
  private final ConcurrentLinkedQueue<T> onNextQueue = new ConcurrentLinkedQueue<>();

  protected SubscriptionSupport(Subscriber<? super T> subscriber) {
    this.subscriber = subscriber;
    subscriber.onSubscribe(this);
  }

  @Override
  public final void request(long n) {
    if (n < 1) {
      throw new IllegalArgumentException("3.9 While the Subscription is not cancelled, Subscription.request(long n) MUST throw a java.lang.IllegalArgumentException if the argument is <= 0.");
    }

    if (!stopped.get()) {
      waitingRequests.addAndGet(n);
      if (started.get()) {
        drainRequests();
      }
    }
  }

  protected boolean isStopped() {
    return stopped.get();
  }

  private void drainRequests() {
    if (drainingRequests.compareAndSet(false, true)) {
      try {
        long n = waitingRequests.getAndSet(0);
        while (n > 0) {
          doRequest(n);
          n = waitingRequests.getAndSet(0);
        }
      } finally {
        drainingRequests.set(false);
      }
      if (waitingRequests.get() > 0) {
        drainRequests();
      }
    }
  }

  protected abstract void doRequest(long n);

  @Override
  public final void cancel() {
    stopped.set(true);
    doCancel();
  }

  protected void doCancel() {
    // do nothing
  }

  protected void start() {
    if (started.compareAndSet(false, true)) {
      drainRequests();
    }
  }

  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  private void nextEvent() {
    if (inOnMethod.compareAndSet(false, true)) {
      try {
        T next = onNextQueue.poll();
        while (next != null) {
          subscriber.onNext(next);
          next = onNextQueue.poll();
        }
        if (complete.get()) {
          subscriber.onComplete();
          return;
        }
        Throwable error = this.error.get();
        if (error != null) {
          subscriber.onError(error);
          return;
        }
      } finally {
        inOnMethod.set(false);
      }
      if (!onNextQueue.isEmpty() || complete.get() || error.get() != null) {
        nextEvent();
      }
    }
  }

  public void onNext(T t) {
    if (!stopped.get()) {
      onNextQueue.add(t);
      nextEvent();
    }
  }

  public void onError(Throwable t) {
    if (stopped.compareAndSet(false, true)) {
      error.set(t);
      nextEvent();
    }
  }

  public void onComplete() {
    if (stopped.compareAndSet(false, true)) {
      complete.set(true);
      nextEvent();
    }
  }

}
