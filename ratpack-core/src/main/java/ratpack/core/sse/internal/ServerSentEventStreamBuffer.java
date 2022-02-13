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

package ratpack.core.sse.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import ratpack.exec.stream.bytebuf.internal.ByteBufBufferingSubscription;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Note: this is not a general purpose publisher as it assumes all signals are from a single thread,
 * and that the given executor uses that same thread.
 * <p>
 * This is safe for single execution/channel based usage.
 */
public class ServerSentEventStreamBuffer implements Publisher<ByteBuf> {

  private final Publisher<? extends ByteBuf> upstream;
  private final ScheduledExecutorService executor;
  private final ServerSentEventStreamBufferSettings bufferSettings;
  private final Clock clock;
  private final ByteBufAllocator byteBufAllocator;

  public ServerSentEventStreamBuffer(
    Publisher<? extends ByteBuf> upstream,
    ScheduledExecutorService executor,
    ByteBufAllocator byteBufAllocator,
    ServerSentEventStreamBufferSettings bufferSettings,
    Clock clock
  ) {
    this.upstream = upstream;
    this.executor = executor;
    this.byteBufAllocator = byteBufAllocator;
    this.bufferSettings = bufferSettings;
    this.clock = clock;
  }

  @Override
  public void subscribe(Subscriber<? super ByteBuf> s) {
    long flushFrequencyNanos = bufferSettings.window.toNanos();

    s.onSubscribe(new ByteBufBufferingSubscription(upstream, s, byteBufAllocator, bufferSettings.events, bufferSettings.bytes) {

      ScheduledFuture<?> checkFuture;
      long lastFlushAt;
      boolean needsFlush;

      @Override
      protected void onCancel() {
        shutdown();
        super.onCancel();
      }

      private void shutdown() {
        if (checkFuture != null) {
          checkFuture.cancel(false);
          checkFuture = null;
        }
      }

      @Override
      public void emitError(Throwable error) {
        shutdown();
        super.emitError(error);
      }

      @Override
      public void emitComplete() {
        shutdown();
        super.emitComplete();
      }

      @Override
      protected void onConnected() {
        if (flushFrequencyNanos > 0) {
          long now = System.nanoTime();
          lastFlushAt = now;
          scheduleCheck(now);
        }
      }

      private void check() {
        long nowNanos = clock.nanoTime();
        long sinceLastFlushNanos = nowNanos - lastFlushAt;
        boolean flushIsDue = sinceLastFlushNanos >= flushFrequencyNanos;

        if (flushIsDue) {
          if (isEmpty()) {
            needsFlush = true;
            return;
          } else {
            flush();
          }
        }

        scheduleCheck(nowNanos);
      }

      private void scheduleCheck(long nowNanos) {
        long scheduleFor = nowNanos - (lastFlushAt + flushFrequencyNanos);
        checkFuture = executor.schedule(this::check, scheduleFor, TimeUnit.NANOSECONDS);
      }

      @Override
      protected boolean shouldFlush() {
        return !isEmpty() && needsFlush || super.shouldFlush();
      }

      @Override
      protected void flush() {
        super.flush();
        lastFlushAt = clock.nanoTime();
        if (needsFlush) {
          scheduleCheck(lastFlushAt);
          needsFlush = false;
        }
      }
    });
  }

}
