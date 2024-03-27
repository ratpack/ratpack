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

package ratpack.exec.stream.internal;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.func.Action;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

public abstract class ManagedSubscription<T> implements Subscription {

  private static final int INIT = 0;
  private static final int STOPPED = 1;
  private static final Logger LOGGER = LoggerFactory.getLogger(ManagedSubscription.class);

  private volatile boolean open;
  private volatile long demand;

  private final Action<? super T> disposer;
  protected final Subscriber<? super T> downstream;

  private volatile int state = INIT;

  private volatile boolean emitting;
  private volatile long emitDemand;

  @SuppressWarnings("rawtypes")
  private static final AtomicLongFieldUpdater<ManagedSubscription> EMIT_DEMAND_UPDATER = AtomicLongFieldUpdater.newUpdater(ManagedSubscription.class, "emitDemand");

  @SuppressWarnings("rawtypes")
  private static final AtomicLongFieldUpdater<ManagedSubscription> DEMAND_UPDATER = AtomicLongFieldUpdater.newUpdater(ManagedSubscription.class, "demand");

  @SuppressWarnings("rawtypes")
  private static final AtomicIntegerFieldUpdater<ManagedSubscription> DONE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(ManagedSubscription.class, "state");

  public ManagedSubscription(Subscriber<? super T> downstream, Action<? super T> disposer) {
    this.downstream = downstream;
    this.disposer = disposer;
  }

  @Override
  public final void request(long n) {
    if (n < 1) {
      downstream.onError(new IllegalArgumentException("3.9 While the Subscription is not cancelled, Subscription.request(long n) MUST throw a java.lang.IllegalArgumentException if the argument is <= 0."));
      cancel();
      return;
    }

    if (!open) {
      if (n == Long.MAX_VALUE) {
        open = true;
        DEMAND_UPDATER.set(this, Long.MAX_VALUE);
      } else {
        long newDemand = DEMAND_UPDATER.addAndGet(this, n);
        if (newDemand < 1 || newDemand == Long.MAX_VALUE) {
          open = true;
          n = Long.MAX_VALUE;
          DEMAND_UPDATER.set(this, Long.MAX_VALUE);
        }
      }

      if (emitting) {
        EMIT_DEMAND_UPDATER.addAndGet(this, n);
      } else {
        onRequest(n);
      }
    }
  }

  public long getDemand() {
    return isDone() ? 0 : demand;
  }

  protected boolean shouldEmit() {
    return isDone() || demand > 0;
  }

  protected boolean isDone() {
    return state == STOPPED;
  }

  protected boolean isOpen() {
    return open;
  }

  public boolean hasDemand() {
    return getDemand() > 0;
  }

  protected abstract void onRequest(long n);

  protected abstract void onCancel();

  public void emitNext(T item) {
    if (isDone()) {
      dispose(item);
    } else {
      if (!open) {
        DEMAND_UPDATER.decrementAndGet(this);
      }
      emitting = true;
      try {
        downstream.onNext(item);
      } finally {
        emitting = false;
        if (emitDemand != 0) {
          onRequest(EMIT_DEMAND_UPDATER.getAndSet(this, 0));
        }
      }
    }
  }

  public void emitError(Throwable error) {
    if (fireDone()) {
      downstream.onError(error);
    }
  }

  public void emitComplete() {
    if (fireDone()) {
      downstream.onComplete();
    }
  }

  protected void dispose(T item) {
    doDispose(disposer, item);
  }

  protected static <T> void doDispose(Action<? super T> disposer, T item) {
    try {
      disposer.execute(item);
    } catch (Exception e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn("exception raised disposing of " + item + " - will be ignored", e);
      }
    }
  }

  private boolean fireDone() {
    return DONE_UPDATER.getAndSet(this, STOPPED) != STOPPED;
  }

  @Override
  public void cancel() {
    onCancel();
    fireDone();
  }

}
