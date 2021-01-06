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

import com.google.common.base.Stopwatch;
import io.netty.util.internal.PlatformDependent;
import ratpack.exec.*;
import ratpack.func.Function;

import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class CachingUpstream<T> implements Upstream<T> {

  private Upstream<? extends T> upstream;

  private final AtomicReference<Loading> loadingRef = new AtomicReference<>(new Loading());
  private final Function<? super ExecResult<T>, Duration> ttlFunc;

  private final AtomicBoolean draining = new AtomicBoolean();
  private final Queue<Downstream<? super T>> waiting = PlatformDependent.newMpscQueue();

  public CachingUpstream(Upstream<? extends T> upstream, Function<? super ExecResult<T>, Duration> ttl) {
    this.upstream = upstream;
    this.ttlFunc = ttl;
  }

  private void tryDrain() {
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
      if (!waiting.isEmpty() && loadingRef.get().getState() != LoadingState.PENDING) {
        tryDrain();
      }
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

    if (ttl.isNegative()) {
      upstream = null; // release
    }

    loading.cached = new Cached<>(result, ttl);
    downstream.accept(result);

    tryDrain();
  }

  private static class Cached<T> {
    final ExecResult<T> result;
    private final Duration ttl;
    private final Stopwatch stopwatch;

    Cached(ExecResult<T> result, Duration ttl) {
      this.result = result;
      this.ttl = ttl;
      if (!ttl.isNegative()) {
        this.stopwatch = Stopwatch.createStarted();
      } else {
        this.stopwatch = null;
      }
    }

    boolean isExpired() {
      return stopwatch != null && stopwatch.elapsed().compareTo(ttl) >= 0;
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
        if (cached.isExpired()) {
          return LoadingState.EXPIRED;
        } else {
          return LoadingState.LOADED;
        }
      }
    }

  }

}

