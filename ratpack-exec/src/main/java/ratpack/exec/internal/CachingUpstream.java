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

package ratpack.exec.internal;

import com.google.common.annotations.VisibleForTesting;
import io.netty.util.internal.PlatformDependent;
import ratpack.exec.*;
import ratpack.func.Function;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class CachingUpstream<T> implements Upstream<T> {

  private Upstream<? extends T> upstream;

  private final Clock clock;
  private final AtomicReference<Cached<? extends T>> ref = new AtomicReference<>();
  private final Function<? super ExecResult<T>, Duration> ttlFunc;

  private final AtomicBoolean pending = new AtomicBoolean();
  private final AtomicBoolean draining = new AtomicBoolean();
  private final Queue<Downstream<? super T>> waiting = PlatformDependent.newMpscQueue();

  public CachingUpstream(Upstream<? extends T> upstream, Function<? super ExecResult<T>, Duration> ttl) {
    this(upstream, ttl, Clock.systemUTC());
  }

  @VisibleForTesting
  CachingUpstream(Upstream<? extends T> upstream, Function<? super ExecResult<T>, Duration> ttl, Clock clock) {
    this.upstream = upstream;
    this.ttlFunc = ttl;
    this.clock = clock;
  }

  private void tryDrain() {
    if (draining.compareAndSet(false, true)) {
      try {
        Cached<? extends T> cached = ref.get();
        if (needsFetch(cached)) {
          if (pending.compareAndSet(false, true)) {
            Downstream<? super T> downstream = waiting.poll();
            if (downstream == null) {
              pending.set(false);
            } else {
              try {
                yield(downstream);
              } catch (Throwable e) {
                receiveResult(downstream, ExecResult.of(Result.error(e)));
              }
            }
          }
        } else {
          Downstream<? super T> downstream = waiting.poll();
          while (downstream != null) {
            downstream.accept(cached.result);
            downstream = waiting.poll();
          }
        }
      } finally {
        draining.set(false);
      }
    }

    if (!waiting.isEmpty() && !pending.get() && needsFetch(ref.get())) {
      tryDrain();
    }
  }

  private boolean needsFetch(Cached<? extends T> cached) {
    return cached == null || (cached.expireAt != null && cached.expireAt.isBefore(clock.instant()));
  }

  private void yield(final Downstream<? super T> downstream) throws Exception {
    upstream.connect(new Downstream<T>() {
      public void error(Throwable throwable) {
        receiveResult(downstream, ExecResult.of(Result.<T>error(throwable)));
      }

      @Override
      public void success(T value) {
        receiveResult(downstream, ExecResult.of(Result.success(value)));
      }

      @Override
      public void complete() {
        receiveResult(downstream, CompleteExecResult.get());
      }
    });
  }

  @Override
  public void connect(Downstream<? super T> downstream) throws Exception {
    Cached<? extends T> cached = this.ref.get();
    if (needsFetch(cached)) {
      Promise.<T>async(d -> {
        waiting.add(d);
        tryDrain();
      }).result(downstream::accept);
    } else {
      downstream.accept(cached.result);
    }
  }

  private void receiveResult(Downstream<? super T> downstream, ExecResult<T> result) {
    Duration ttl = Duration.ofSeconds(0);
    try {
      ttl = ttlFunc.apply(result);
    } catch (Throwable e) {
      if (result.isError()) {
        //noinspection ThrowableResultOfMethodCallIgnored
        result.getThrowable().addSuppressed(e);
      } else {
        result = ExecResult.of(Result.error(e));
      }
    }

    Instant expiresAt;
    if (ttl.isNegative()) {
      expiresAt = null; // eternal
      upstream = null; // release
    } else if (ttl.isZero()) {
      expiresAt = clock.instant().minus(Duration.ofSeconds(1));
    } else {
      expiresAt = clock.instant().plus(ttl);
    }

    ref.set(new Cached<>(result, expiresAt));
    pending.set(false);

    downstream.accept(result);

    tryDrain();
  }

  private static class Cached<T> {
    final ExecResult<T> result;
    final Instant expireAt;

    Cached(ExecResult<T> result, Instant expireAt) {
      this.result = result;
      this.expireAt = expireAt;
    }
  }

}

