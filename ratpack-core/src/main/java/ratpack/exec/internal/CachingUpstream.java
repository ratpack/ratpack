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
import ratpack.func.Predicate;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class CachingUpstream<T> implements Upstream<T> {

  private Upstream<? extends T> upstream;

  private final AtomicReference<ExecResult<? extends T>> resultRef = new AtomicReference<>();
  private final Predicate<? super ExecResult<T>> predicate;

  private final AtomicBoolean pending = new AtomicBoolean();
  private final AtomicBoolean draining = new AtomicBoolean();
  private final Queue<Downstream<? super T>> waiting = PlatformDependent.newMpscQueue();

  public CachingUpstream(Upstream<? extends T> upstream, Predicate<? super ExecResult<T>> predicate) {
    this.upstream = upstream;
    this.predicate = predicate;
  }

  private void tryDrain() {
    if (draining.compareAndSet(false, true)) {
      try {
        ExecResult<? extends T> result = this.resultRef.get();
        if (result == null) {
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
            downstream.accept(result);
            downstream = waiting.poll();
          }
        }
      } finally {
        draining.set(false);
      }
    }

    if (!waiting.isEmpty() && (resultRef.get() != null || !pending.get())) {
      tryDrain();
    }
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
    ExecResult<? extends T> result = this.resultRef.get();
    if (result == null) {
      Promise.<T>async(d -> {
        waiting.add(d);
        tryDrain();
      }).result(downstream::accept);
    } else {
      downstream.accept(result);
    }
  }

  private void receiveResult(Downstream<? super T> downstream, ExecResult<T> result) {
    boolean shouldCache = false;
    try {
      shouldCache = predicate.apply(result);
    } catch (Throwable e) {
      if (result.isError()) {
        //noinspection ThrowableResultOfMethodCallIgnored
        result.getThrowable().addSuppressed(e);
      } else {
        result = ExecResult.of(Result.error(e));
      }
    }

    if (shouldCache) {
      this.resultRef.set(result);
      this.upstream = null; // release
    } else {
      pending.set(false);
    }

    downstream.accept(result);

    tryDrain();
  }

}

