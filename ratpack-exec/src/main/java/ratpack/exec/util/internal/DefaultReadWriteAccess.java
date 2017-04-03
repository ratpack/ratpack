/*
 * Copyright 2017 the original author or authors.
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

package ratpack.exec.util.internal;

import io.netty.util.concurrent.ScheduledFuture;
import ratpack.exec.Downstream;
import ratpack.exec.Promise;
import ratpack.exec.Upstream;
import ratpack.exec.internal.Continuation;
import ratpack.exec.internal.DefaultExecution;
import ratpack.exec.util.ReadWriteAccess;

import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultReadWriteAccess implements ReadWriteAccess {

  private final Queue<Access<?>> queue = new ConcurrentLinkedQueue<>();
  private final AtomicBoolean draining = new AtomicBoolean();
  private final AtomicInteger activeReaders = new AtomicInteger();
  private final Duration defaultTimeout;

  private AtomicReference<Access<?>> pendingWriteRef = new AtomicReference<>();

  public DefaultReadWriteAccess(Duration defaultTimeout) {
    if (defaultTimeout.isNegative()) {
      throw new IllegalArgumentException("defaultTimeout must not be negative");
    }

    this.defaultTimeout = defaultTimeout;
  }

  @Override
  public Duration getDefaultTimeout() {
    return defaultTimeout;
  }

  @Override
  public <T> Promise<T> read(Promise<T> promise) {
    return promise.transform(up -> down -> new Access<T>(true, up, defaultTimeout, down));
  }

  @Override
  public <T> Promise<T> read(Promise<T> promise, Duration timeout) {
    return promise.transform(up -> down -> new Access<T>(true, up, timeout, down));
  }

  @Override
  public <T> Promise<T> write(Promise<T> promise) {
    return promise.transform(up -> down -> new Access<T>(false, up, defaultTimeout, down));
  }

  @Override
  public <T> Promise<T> write(Promise<T> promise, Duration timeout) {
    return promise.transform(up -> down -> new Access<T>(false, up, timeout, down));
  }

  private class Access<T> {

    private final boolean read;
    private final Upstream<? extends T> upstream;
    private final Duration timeout;
    private final Downstream<? super T> downstream;
    private final DefaultExecution execution;
    private boolean fired;

    private Continuation continuation;
    private ScheduledFuture<?> timeoutFuture;

    private Access(boolean read, Upstream<? extends T> upstream, Duration timeout, Downstream<? super T> downstream) {
      if (timeout.isNegative()) {
        throw new IllegalArgumentException("Timeout value must not be negative");
      }

      this.read = read;
      this.upstream = upstream;
      this.timeout = timeout;
      this.downstream = downstream;
      this.execution = DefaultExecution.get();

      execution.delimit(e -> {
        relinquish(false);
        if (fire()) {
          downstream.error(e);
        }
      }, continuation -> {
        if (!timeout.isZero()) {
          timeoutFuture = execution.getEventLoop().schedule(this::timeout, timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        this.continuation = continuation;
        queue.add(this);
        drain();
      });
    }

    private boolean fire() {
      //noinspection SimplifiableIfStatement
      if (fired) {
        return false;
      } else {
        return fired = true;
      }
    }

    private void timeout() {
      if (fire()) {
        continuation.resume(() -> {
          drain();
          downstream.error(new TimeoutException("Could not acquire " + (read ? "read" : "write") + " access within " + timeout));
        });
      }
    }

    private void access() {
      if (read) {
        activeReaders.incrementAndGet();
      }

      if (fire()) {
        if (timeoutFuture != null) {
          timeoutFuture.cancel(false);
        }
        continuation.resume(() ->
          upstream.connect(new Downstream<T>() {
            @Override
            public void success(T value) {
              relinquish(false);
              downstream.success(value);
            }

            @Override
            public void error(Throwable throwable) {
              relinquish(false);
              downstream.error(throwable);
            }

            @Override
            public void complete() {
              relinquish(false);
              downstream.complete();
            }
          })
        );
      } else {
        relinquish(true);
      }
    }

    private void relinquish(boolean didTimeout) {
      if (read) {
        if (activeReaders.decrementAndGet() == 0) {
          Access<?> pendingWrite = pendingWriteRef.getAndSet(null);
          if (pendingWrite != null) {
            pendingWrite.access();
            return;
          }
        }
      } else {
        draining.set(false);
      }

      drain();
    }
  }

  private void drain() {
    if (draining.compareAndSet(false, true)) {
      Access<?> access = queue.poll();
      while (access != null) {
        if (access.read) {
          access.access();
          access = queue.poll();
        } else {
          if (activeReaders.get() == 0) {
            access.access();
          } else {
            pendingWriteRef.set(access);
            if (activeReaders.get() == 0) {
              access = pendingWriteRef.getAndSet(null);
              if (access != null) {
                access.access();
              }
            }
          }
          return;
        }
      }
      draining.set(false);
      if (!queue.isEmpty()) {
        drain();
      }
    }
  }
}
