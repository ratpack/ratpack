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

package ratpack.sse.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Note: this is not a general purpose publisher as it assumes all signals are from a single thread,
 * and that the given executor uses that same thread.
 *
 * This is safe for single execution/channel based usage.
 */
public class ServerSentEventStreamKeepAlive implements Publisher<ByteBuf> {

  private static final ByteBuf HEARTBEAT = Unpooled.unreleasableBuffer(
    Unpooled.wrappedBuffer(": keepalive heartbeat\n\n".getBytes(StandardCharsets.UTF_8))
  );

  private final Publisher<? extends ByteBuf> upstream;
  private final ScheduledExecutorService executor;
  private final Duration heartBeatFrequency;
  private final Clock clock;

  public ServerSentEventStreamKeepAlive(
    Publisher<? extends ByteBuf> upstream,
    ScheduledExecutorService executor,
    Duration heartBeatFrequency,
    Clock clock
  ) {
    this.upstream = upstream;
    this.executor = executor;
    this.heartBeatFrequency = heartBeatFrequency;
    this.clock = clock;
  }

  @Override
  public void subscribe(Subscriber<? super ByteBuf> downstream) {
    long heartbeatFrequencyNanos = heartBeatFrequency.toNanos();
    upstream.subscribe(new Subscriber<ByteBuf>() {
      private Subscription subscription;
      private ScheduledFuture<?> checkFuture;
      long lastWriteAt;
      long demand;
      long demandSurplusSent;
      boolean needsHeartbeat;

      private void stop() {
        if (checkFuture != null) {
          checkFuture.cancel(false);
          checkFuture = null;
        }
      }

      @Override
      public void onSubscribe(Subscription s) {
        this.subscription = s;
        downstream.onSubscribe(new Subscription() {

          private void scheduleCheck(long inNanos) {
            checkFuture = executor.schedule(this::check, inNanos, TimeUnit.NANOSECONDS);
          }

          private void check() {
            long nowNanos = clock.nanoTime();
            long heartbeatDue = lastWriteAt + heartbeatFrequencyNanos;
            if (heartbeatDue <= nowNanos) {
              if (demand > demandSurplusSent) {
                emitHeartbeat();
              } else {
                needsHeartbeat = true;
              }
            } else {
              scheduleCheck(heartbeatDue - nowNanos);
            }
          }

          private void emitHeartbeat() {
            needsHeartbeat = false;
            ++demandSurplusSent;
            lastWriteAt = clock.nanoTime();
            downstream.onNext(HEARTBEAT.touch());
            scheduleCheck(heartbeatFrequencyNanos);
          }

          @Override
          public void request(long request) {
            if (checkFuture == null) {
              lastWriteAt = clock.nanoTime();
              scheduleCheck(heartbeatFrequencyNanos);
            }

            long adjustment = Math.min(request, demandSurplusSent);
            demandSurplusSent -= adjustment;
            long adjustedRequest = request - adjustment;
            demand += adjustedRequest;

            if (needsHeartbeat && demand > demandSurplusSent) {
              emitHeartbeat();
            }

            if (adjustedRequest > 0) {
              subscription.request(adjustedRequest);
            }
          }

          @Override
          public void cancel() {
            stop();
            subscription.cancel();
          }
        });
      }

      @Override
      public void onNext(ByteBuf byteBuf) {
        --demand;
        needsHeartbeat = false;
        lastWriteAt = clock.nanoTime();
        downstream.onNext(byteBuf.touch());
      }

      @Override
      public void onError(Throwable t) {
        stop();
        downstream.onError(t);
      }

      @Override
      public void onComplete() {
        stop();
        downstream.onComplete();
      }
    });
  }

}
