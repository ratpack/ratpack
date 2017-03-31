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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public abstract class ManagedSubscription<T> implements Subscription {

  private static final Logger LOGGER = LoggerFactory.getLogger(ManagedSubscription.class);

  private volatile boolean open;
  private final AtomicLong demand = new AtomicLong();

  private final Action<? super T> disposer;
  private final Subscriber<? super T> subscriber;

  private final AtomicBoolean done = new AtomicBoolean();

  public ManagedSubscription(Subscriber<? super T> subscriber, Action<? super T> disposer) {
    this.subscriber = subscriber;
    this.disposer = disposer;
  }

  @Override
  public final void request(long n) {
    if (n < 1) {
      subscriber.onError(new IllegalArgumentException("3.9 While the Subscription is not cancelled, Subscription.request(long n) MUST throw a java.lang.IllegalArgumentException if the argument is <= 0."));
      cancel();
      return;
    }

    if (!open) {
      if (n == Long.MAX_VALUE) {
        open = true;
        demand.set(Long.MAX_VALUE);
      } else {
        long newDemand = demand.addAndGet(n);
        if (newDemand < 1 || newDemand == Long.MAX_VALUE) {
          open = true;
          n = Long.MAX_VALUE;
          demand.set(Long.MAX_VALUE);
        }
      }

      onRequest(n);
    }
  }

  protected long getDemand() {
    return isDone() ? 0 : demand.get();
  }

  protected boolean shouldEmit() {
    return isDone() || demand.get() > 0;
  }

  protected boolean isDone() {
    return done.get();
  }

  protected boolean hasDemand() {
    return getDemand() > 0;
  }

  protected abstract void onRequest(long n);

  protected abstract void onCancel();

  protected void emitNext(T item) {
    if (isDone()) {
      dispose(item);
    } else {
      if (!open) {
        demand.decrementAndGet();
      }
      subscriber.onNext(item);
    }
  }

  protected void emitError(Throwable error) {
    if (fireDone()) {
      subscriber.onError(error);
    }
  }

  protected void emitComplete() {
    if (fireDone()) {
      subscriber.onComplete();
    }
  }

  protected void dispose(T item) {
    try {
      disposer.execute(item);
    } catch (Exception e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn("exception raised disposing of " + item + " - will be ignored", e);
      }
    }
  }

  private boolean fireDone() {
    return done.compareAndSet(false, true);
  }

  @Override
  public void cancel() {
    onCancel();
    fireDone();
  }

}
