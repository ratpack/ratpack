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

import io.netty.util.internal.PlatformDependent;
import ratpack.exec.*;
import ratpack.func.Function;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class CachingUpstream<T> implements Upstream<T> {

  private Upstream<? extends T> upstream;

  private final Clock clock;
  private final AtomicReference<Loading> loadingRef = new AtomicReference<>(new Loading());
  private final Function<? super ExecResult<T>, Duration> ttlFunc;

  private final AtomicBoolean draining = new AtomicBoolean();
  private final Queue<Downstream<? super T>> waiting = PlatformDependent.newMpscQueue();

  public CachingUpstream(Upstream<? extends T> upstream, Function<? super ExecResult<T>, Duration> ttl) {
    this(upstream, ttl, Clock.systemUTC());
  }

  private CachingUpstream(Upstream<? extends T> upstream, Function<? super ExecResult<T>, Duration> ttl, Clock clock) {
    this.upstream = upstream;
    this.ttlFunc = ttl;
    this.clock = clock;
  }

  private boolean shouldDrain() {
    return !waiting.isEmpty() && loadingRef.get().getState() != LoadingState.PENDING;
  }

  // This uses lazy evaluation of lambdas in an infinite collection in order to
  // create a tail-call optimization (http://blog.agiledeveloper.com/2013/01/functional-programming-in-java-is-quite.html)
  private void tryDrain() {
    Stream.iterate(drain(), Drainer::get).filter(Drainer::done).findFirst();
  }

  private Drainer drain() {
    if (!shouldDrain()) {
      return new DrainerEnd();
    } else {
      return () -> {
        if (draining.compareAndSet(false, true)) {
          try {
            if (!waiting.isEmpty()) {
              Loading loading = loadingRef.get();
              LoadingState state = loading.getState();
              if (state == LoadingState.INIT) {
                startLoad(loading);
              } else if (state == LoadingState.EXPIRED) {
                Loading newLoading = new Loading();
                loadingRef.compareAndSet(loading, newLoading);
                startLoad(loadingRef.get());
              } else if (state == LoadingState.LOADED) {
                Downstream<? super T> downstream = waiting.poll();
                while (downstream != null) {
                  downstream.accept(loading.cached.result);
                  downstream = waiting.poll();
                }
              }
            }
          } finally {
            draining.set(false);
          }
        }
        return drain();
      };
    }
  }

  private void startLoad(Loading loading) {
    if (loading.pending.compareAndSet(false, true)) {
      Downstream<? super T> downstream = waiting.poll();
      try {
        yield(loading, downstream);
      } catch (Throwable e) {
        receiveResult(loading, downstream, ExecResult.of(Result.error(e)));
      }
    }
  }

  private void yield(Loading loading, Downstream<? super T> downstream) throws Exception {
    upstream.connect(new Downstream<T>() {
      public void error(Throwable throwable) {
        receiveResult(loading, downstream, ExecResult.of(Result.error(throwable)));
      }

      @Override
      public void success(T value) {
        receiveResult(loading, downstream, ExecResult.of(Result.success(value)));
      }

      @Override
      public void complete() {
        receiveResult(loading, downstream, CompleteExecResult.get());
      }
    });
  }

  @Override
  public void connect(Downstream<? super T> downstream) {
    Loading loading = this.loadingRef.get();
    if (loading.getState() == LoadingState.LOADED) {
      downstream.accept(loading.cached.result);
    } else {
      Promise.<T>async(d -> {
        waiting.add(d);
        tryDrain();
      }).result(downstream::accept);
    }
  }

  private void receiveResult(Loading loading, Downstream<? super T> downstream, ExecResult<T> result) {
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

    loading.cached = new Cached<>(result, expiresAt);
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

  enum LoadingState {
    INIT,
    PENDING,
    EXPIRED,
    LOADED
  }

  private class Loading {
    volatile Cached<T> cached;
    final AtomicBoolean pending = new AtomicBoolean();

    private LoadingState getState() {
      if (cached == null) {
        if (pending.get()) {
          return LoadingState.PENDING;
        } else {
          return LoadingState.INIT;
        }
      } else {
        if (cached.expireAt == null || cached.expireAt.isAfter(clock.instant())) {
          return LoadingState.LOADED;
        } else {
          return LoadingState.EXPIRED;
        }
      }
    }

  }

  @FunctionalInterface
  interface Drainer {
    Drainer get();

    default boolean done() {
      return false;
    }
  }

  static class DrainerEnd implements Drainer {
    public Drainer get() {
      throw new RuntimeException("should not drain");
    }

    public boolean done() {
      return true;
    }
  }

}

