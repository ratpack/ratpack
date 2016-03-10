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

import io.netty.util.internal.PlatformDependent;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public abstract class SubscriptionSupport<T> implements Subscription {

  private Subscriber<? super T> subscriber;

  private final AtomicBoolean started = new AtomicBoolean();
  private final AtomicBoolean stopped = new AtomicBoolean();

  private final AtomicLong waitingRequests = new AtomicLong(Long.MIN_VALUE);
  private final AtomicBoolean drainingRequests = new AtomicBoolean();

  private final AtomicBoolean complete = new AtomicBoolean();
  private final AtomicReference<Throwable> error = new AtomicReference<>();

  private final AtomicBoolean inOnMethod = new AtomicBoolean();
  private final Queue<T> onNextQueue = PlatformDependent.newMpscQueue();

  protected SubscriptionSupport(Subscriber<? super T> subscriber) {
    this.subscriber = subscriber;
    subscriber.onSubscribe(this);
  }

  @Override
  public final void request(long n) {
    if (n < 1) {
      onError(new IllegalArgumentException("3.9 While the Subscription is not cancelled, Subscription.request(long n) MUST throw a java.lang.IllegalArgumentException if the argument is <= 0."));
      cancel();
    }

    if (!stopped.get()) {
      long waiting = waitingRequests.get();
      if (waiting < 0) {
        waitingRequests.addAndGet(n);
      }
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
        long n = waitingRequests.getAndSet(Long.MIN_VALUE);
        if (n == Long.MAX_VALUE) {
          return;
        }

        while (!stopped.get() && n > Long.MIN_VALUE) {
          if (n >= 0) {
            doRequest(Long.MAX_VALUE);
            waitingRequests.set(Long.MAX_VALUE);
            return;
          } else {
            long r = n + Long.MIN_VALUE;
            doRequest(r);
            n = waitingRequests.getAndSet(Long.MIN_VALUE);
          }
        }
      } finally {
        drainingRequests.set(false);
      }
      if (waitingRequests.get() > Long.MIN_VALUE) {
        drainRequests();
      }
    }
  }

  protected abstract void doRequest(long n);

  @Override
  public final void cancel() {
    stopped.set(true);
    subscriber = null;
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

  private void drain() {
    if (inOnMethod.compareAndSet(false, true)) {
      try {
        while (true) {
          Throwable error = this.error.get();
          if (error != null) {
            subscriber.onError(error);
            return;
          }
          T next = onNextQueue.poll();
          if (next == null) {
            break;
          } else {
            subscriber.onNext(next);
          }
        }
        if (complete.get()) {
          subscriber.onComplete();
          return;
        }
      } finally {
        inOnMethod.set(false);
      }
      //noinspection ThrowableResultOfMethodCallIgnored
      if (!onNextQueue.isEmpty() || complete.get() || error.get() != null) {
        drain();
      }
    }
  }

  public void onNext(T t) {
    if (!stopped.get()) {
      onNextQueue.add(t);
      drain();
    }
  }

  public void onError(Throwable t) {
    if (stopped.compareAndSet(false, true)) {
      error.set(t);
      drain();
    }
  }

  public void onComplete() {
    if (stopped.compareAndSet(false, true)) {
      complete.set(true);
      drain();
    }
  }

}
