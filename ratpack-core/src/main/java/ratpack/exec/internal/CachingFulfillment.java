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

import ratpack.exec.Fulfiller;
import ratpack.exec.Result;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CachingFulfillment<T> implements Consumer<Fulfiller<T>> {

  private final Consumer<? super Fulfiller<T>> delegate;
  private final Supplier<? extends ExecutionBacking> executionSupplier;
  private final AtomicBoolean fired = new AtomicBoolean();
  private final Queue<Job> waiting = new ConcurrentLinkedQueue<>();
  private final AtomicBoolean draining = new AtomicBoolean();
  private final AtomicReference<Result<T>> result = new AtomicReference<>();

  public CachingFulfillment(Consumer<? super Fulfiller<T>> delegate, Supplier<? extends ExecutionBacking> executionSupplier) {
    this.delegate = delegate;
    this.executionSupplier = executionSupplier;
  }

  private class Job {
    final Fulfiller<? super T> fulfiller;
    final ExecutionBacking.StreamHandle streamHandle;

    private Job(Fulfiller<? super T> fulfiller, ExecutionBacking.StreamHandle streamHandle) {
      this.fulfiller = fulfiller;
      this.streamHandle = streamHandle;
    }
  }

  private void tryDrain() {
    if (draining.compareAndSet(false, true)) {
      try {
        Result<T> result = this.result.get();

        Job job = waiting.poll();
        while (job != null) {
          Job finalJob = job;
          job.streamHandle.complete(() -> finalJob.fulfiller.accept(result));
          job = waiting.poll();
        }
      } finally {
        draining.set(false);
      }
    }
    if (!draining.get() && !waiting.isEmpty()) {
      tryDrain();
    }
  }

  @Override
  public void accept(Fulfiller<T> fulfiller) {
    if (fired.compareAndSet(false, true)) {
      delegate.accept(new Fulfiller<T>() {
        @Override
        public void error(Throwable throwable) {
          result.set(Result.<T>failure(throwable));
          fulfiller.error(throwable);
          executionSupplier.get().getEventLoop().execute(CachingFulfillment.this::tryDrain);
        }

        @Override
        public void success(T value) {
          result.set(Result.success(value));
          fulfiller.success(value);
          executionSupplier.get().getEventLoop().execute(CachingFulfillment.this::tryDrain);
        }
      });
    } else {
      executionSupplier.get().streamSubscribe((streamHandle) -> {
        waiting.add(new Job(fulfiller, streamHandle));
        if (result.get() != null) {
          tryDrain();
        }
      });
    }
  }
}

