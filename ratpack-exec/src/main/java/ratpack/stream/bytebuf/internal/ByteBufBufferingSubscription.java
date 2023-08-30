/*
 * Copyright 2021 the original author or authors.
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

package ratpack.stream.bytebuf.internal;

import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import ratpack.func.Action;
import ratpack.stream.internal.MiddlemanSubscription;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public abstract class ByteBufBufferingSubscription<T> extends MiddlemanSubscription<T, ByteBuf> {

  private final ScheduledExecutorService executor;
  private final LongSupplier clock;

  private final long flushFrequencyNanos;
  private ScheduledFuture<?> flushCheckFuture;
  private long lastFlushAt;
  private boolean needsFlush;

  private final long keepAliveFrequencyNanos;
  private final ByteBuf keepAliveItem;
  private ScheduledFuture<?> keepAliveCheckFuture;
  private boolean needsKeepAlive;
  private long lastEmitAt;
  private int keepAlivesSentSinceLastItem;

  public ByteBufBufferingSubscription(
    Publisher<? extends T> upstream,
    Action<? super T> upstreamDisposer,
    Subscriber<? super ByteBuf> subscriber,
    ScheduledExecutorService executor,
    LongSupplier clock,
    Duration flushFrequency,
    Duration keepAliveFrequency,
    ByteBuf keepAliveItem
  ) {
    super(subscriber, ByteBuf::release, upstream, upstreamDisposer);
    this.executor = executor;
    this.clock = clock;
    this.flushFrequencyNanos = flushFrequency.toNanos();
    this.keepAliveFrequencyNanos = keepAliveFrequency.toNanos();
    this.keepAliveItem = keepAliveItem;
  }

  @Override
  public void connect() {
    super.connect();
  }

  @Override
  protected void onConnected() {
    lastFlushAt = lastEmitAt = System.nanoTime();
    if (flushFrequencyNanos > 0) {
      scheduleFlushCheck(flushFrequencyNanos);
    }
    if (keepAliveFrequencyNanos > 0) {
      scheduleKeepAliveCheck(keepAliveFrequencyNanos);
    }
    super.onConnected();
  }

  @Override
  protected void onRequest(long n) {
    n = n - keepAlivesSentSinceLastItem;
    if (n > 0) {
      super.onRequest(n);
    }

    if (needsKeepAlive) {
      emitKeepAlive();
    } else if (keepAliveCheckFuture == null && keepAliveFrequencyNanos > 0) {
      scheduleKeepAliveCheck(keepAliveFrequencyNanos);
    }
  }

  private void scheduleKeepAliveCheck(long inNanos) {
    if (!isDone() && keepAliveCheckFuture == null) {
      keepAliveCheckFuture = executor.schedule(this::keepAliveCheck, inNanos, TimeUnit.NANOSECONDS);
    }
  }

  private void keepAliveCheck() {
    keepAliveCheckFuture = null;

    long now = clock.getAsLong();
    long heartbeatDue = lastEmitAt + keepAliveFrequencyNanos;
    needsKeepAlive = heartbeatDue <= now;

    if (needsKeepAlive) {
      if (hasDemand()) {
        emitKeepAlive();
      }
    } else {
      long nextHeartbeatCheckNanos = heartbeatDue - now;
      if (nextHeartbeatCheckNanos > 0) {
        scheduleKeepAliveCheck(nextHeartbeatCheckNanos);
      }
    }
  }

  private void emitKeepAlive() {
    ++keepAlivesSentSinceLastItem;
    emit(keepAliveItem.retainedSlice());
    scheduleKeepAliveCheck(keepAliveFrequencyNanos);
  }

  private void flushCheck() {
    long now = clock.getAsLong();
    long sinceLastFlushNanos = now - lastFlushAt;
    boolean flushIsDue = sinceLastFlushNanos >= flushFrequencyNanos;

    if (flushIsDue) {
      if (isEmpty()) {
        needsFlush = true;
        return;
      } else {
        doFlush(now);
      }
    }

    scheduleFlushCheck(now - (lastFlushAt + flushFrequencyNanos));
  }

  private void doFlush(long now) {
    lastFlushAt = now;
    emitNext(flush());
  }

  private void scheduleFlushCheck(long scheduleFor) {
    if (!isDone()) {
      flushCheckFuture = executor.schedule(this::flushCheck, scheduleFor, TimeUnit.NANOSECONDS);
    }
  }

  @Override
  protected final void receiveNext(T item) {
    buffer(item);
    if (!maybeFlush()) {
      onConsumed();
    }
  }

  @Override
  protected void receiveError(Throwable error) {
    forceFlush();
    super.receiveError(error);
  }

  @Override
  protected void receiveComplete() {
    forceFlush();
    super.receiveComplete();
  }

  protected abstract void buffer(T item);

  @Override
  protected final void onCancel() {
    ScheduledFuture<?> flushCheckFuture = this.flushCheckFuture;
    if (flushCheckFuture != null) {
      flushCheckFuture.cancel(false);
      this.flushCheckFuture = null;
    }

    ScheduledFuture<?> keepAliveCheckFuture = this.keepAliveCheckFuture;
    if (keepAliveCheckFuture != null) {
      keepAliveCheckFuture.cancel(false);
      this.keepAliveCheckFuture = null;
    }

    super.onCancel();
    discard();
  }

  private void forceFlush() {
    if (!isEmpty()) {
      emitNext(flush());
    }
  }

  private boolean maybeFlush() {
    if (shouldFlush()) {
      doFlush(clock.getAsLong());
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void emitNext(ByteBuf item) {
    keepAlivesSentSinceLastItem = 0;
    emit(item);
  }

  private void emit(ByteBuf item) {
    lastEmitAt = clock.getAsLong();
    needsKeepAlive = false;
    super.emitNext(item);
  }

  private boolean shouldFlush() {
    return ((!isEmpty() && needsFlush || needsKeepAlive) || bufferIsFull()) && hasDemand();
  }

  protected abstract boolean bufferIsFull();

  protected abstract ByteBuf flush();

  protected abstract void discard();

  protected abstract boolean isEmpty();

}
