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
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class DefaultReadWriteAccess implements ReadWriteAccess {

  private static final AtomicIntegerFieldUpdater<DefaultReadWriteAccess> DRAINING_UPDATER =
    AtomicIntegerFieldUpdater.newUpdater(DefaultReadWriteAccess.class, "draining");
  private static final AtomicIntegerFieldUpdater<DefaultReadWriteAccess> ACTIVE_READERS_UPDATER =
    AtomicIntegerFieldUpdater.newUpdater(DefaultReadWriteAccess.class, "activeReaders");

  @SuppressWarnings("rawtypes")
  private static final AtomicReferenceFieldUpdater<DefaultReadWriteAccess, Access> PENDING_WRITE_REF_UPDATER =
    AtomicReferenceFieldUpdater.newUpdater(DefaultReadWriteAccess.class, Access.class, "pendingWriteRef");

  @SuppressWarnings("rawtypes")
  private static final AtomicReferenceFieldUpdater<DefaultReadWriteAccess, Queue> QUEUE_UPDATER =
    AtomicReferenceFieldUpdater.newUpdater(DefaultReadWriteAccess.class, Queue.class, "queue");

  private volatile Queue<Access<?>> queue;

  @SuppressWarnings("unused")
  private volatile int draining;

  @SuppressWarnings("unused")
  private volatile int activeReaders;

  private final Duration defaultTimeout;

  private volatile Access<?> pendingWriteRef;

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
        relinquish();
        if (fire()) {
          downstream.error(e);
        }
      }, continuation -> {
        if (!timeout.isZero()) {
          timeoutFuture = execution.getEventLoop().schedule(this::timeout, timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        this.continuation = continuation;
        addToQueue(this);
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
        incActiveReaders();
      }

      if (fire()) {
        if (timeoutFuture != null) {
          timeoutFuture.cancel(false);
        }
        continuation.resume(() ->
          upstream.connect(new Downstream<T>() {
            @Override
            public void success(T value) {
              relinquish();
              downstream.success(value);
            }

            @Override
            public void error(Throwable throwable) {
              relinquish();
              downstream.error(throwable);
            }

            @Override
            public void complete() {
              relinquish();
              downstream.complete();
            }
          })
        );
      } else {
        relinquish();
      }
    }

    private void relinquish() {
      if (read) {
        if (decActiveReaders() == 0) {
          Access<?> pendingWrite = PENDING_WRITE_REF_UPDATER.getAndSet(DefaultReadWriteAccess.this, null);
          if (pendingWrite != null) {
            pendingWrite.access();
            return;
          }
        }
      } else {
        notDraining();
      }

      drain();
    }
  }

  private boolean casQueue(Queue<Access<?>> expected, Queue<Access<?>> queue) {
    return QUEUE_UPDATER.compareAndSet(DefaultReadWriteAccess.this, expected, queue);
  }

  private void drain() {
    if (startDraining()) {
      Queue<Access<?>> queue = getQueue();
      if (queue != null) {
        if (drainQueue(queue)) {
          return;
        }
        resetQueue();
        if (!queue.isEmpty()) {
          if (drainQueue(queue)) {
            return;
          }
        }
      }
      notDraining();
      if (getQueue() != null) {
        drain();
      }
    }
  }

  private void resetQueue() {
    QUEUE_UPDATER.set(this, null);
  }

  private boolean drainQueue(Queue<Access<?>> queue) {
    Access<?> access = queue.poll();
    while (access != null) {
      if (access.read) {
        access.access();
        access = queue.poll();
      } else {
        if (activeReaders() == 0) {
          access.access();
        } else {
          PENDING_WRITE_REF_UPDATER.set(DefaultReadWriteAccess.this, access);
          if (activeReaders() == 0) {
            access = PENDING_WRITE_REF_UPDATER.getAndSet(DefaultReadWriteAccess.this, null);
            if (access != null) {
              access.access();
            }
          }
        }
        return true;
      }
    }
    return false;
  }

  private void addToQueue(Access<?> access) {
    Queue<Access<?>> queue = getQueue();
    if (queue == null) {
      queue = new ConcurrentLinkedQueue<>();
      queue.add(access);
      if (!casQueue(null, queue)) {
        addToQueue(access);
      }
    } else {
      queue.add(access);
      if (!casQueue(queue, queue)) {
        addToQueue(access);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private Queue<Access<?>> getQueue() {
    return QUEUE_UPDATER.get(this);
  }

  private void notDraining() {
    DRAINING_UPDATER.set(this, 0);
  }

  private int decActiveReaders() {
    return ACTIVE_READERS_UPDATER.decrementAndGet(DefaultReadWriteAccess.this);
  }

  private void incActiveReaders() {
    ACTIVE_READERS_UPDATER.incrementAndGet(DefaultReadWriteAccess.this);
  }

  private double activeReaders() {
    return ACTIVE_READERS_UPDATER.get(this);
  }

  private boolean startDraining() {
    return DRAINING_UPDATER.compareAndSet(this, 0, 1);
  }
}
