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

import ratpack.exec.Downstream;
import ratpack.exec.Promise;
import ratpack.exec.Upstream;
import ratpack.exec.internal.Continuation;
import ratpack.exec.internal.DefaultExecution;
import ratpack.exec.util.ReadWriteAccess;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Analogous to {@link ReadWriteLock}, but non blocking.
 * <p>
 * - access is “fair”
 * - access is NOT reentrant
 * <p>
 * Will move to Ratpack 1.5
 */
public class DefaultReadWriteAccess implements ReadWriteAccess {

    private final Queue<Access<?>> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean draining = new AtomicBoolean();
    private final AtomicInteger activeReaders = new AtomicInteger();

    private AtomicReference<Access<?>> pendingWriteRef = new AtomicReference<>();

    private class Access<T> {

        private final boolean read;
        private final Upstream<? extends T> upstream;
        private final Downstream<? super T> downstream;
        private final DefaultExecution execution;

        private Continuation continuation;

        private Access(boolean read, Upstream<? extends T> upstream, Downstream<? super T> downstream) {
            this.read = read;
            this.upstream = upstream;
            this.downstream = downstream;
            this.execution = DefaultExecution.get();

            execution.delimit(e -> {
                relinquish();
                downstream.error(e);
            }, continuation -> {
                this.continuation = continuation;
                queue.add(this);
                drain();
            });
        }

        private void access() {
            if (read) {
                activeReaders.incrementAndGet();
            }

            // Workaround for https://github.com/ratpack/ratpack/issues/1176
            if (execution.isBound()) {
                resume();
            } else {
                execution.getEventLoop().execute(this::resume);
            }
        }

        private void resume() {
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
        }

        private void relinquish() {
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

    @Override
    public <T> Promise<T> read(Promise<T> promise) {
        return promise.transform(up -> down -> new Access<T>(true, up, down));
    }

    @Override
    public <T> Promise<T> write(Promise<T> promise) {
        return promise.transform(up -> down -> new Access<T>(false, up, down));
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
