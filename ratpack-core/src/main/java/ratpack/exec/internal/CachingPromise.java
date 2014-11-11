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

import ratpack.exec.*;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.func.NoArgAction;
import ratpack.func.Predicate;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CachingPromise<T> implements Promise<T> {

  private final Consumer<? super Fulfiller<? super T>> fulfillment;
  private final Supplier<ExecutionBacking> executionSupplier;
  private final Action<? super Throwable> errorHandler;

  private final AtomicBoolean fired = new AtomicBoolean();

  private final Queue<Job> waiting = new ConcurrentLinkedQueue<>();
  private final AtomicBoolean draining = new AtomicBoolean();
  private final AtomicReference<Result<T>> result = new AtomicReference<>();

  public CachingPromise(Consumer<? super Fulfiller<? super T>> fulfillment, Supplier<ExecutionBacking> executionSupplier, Action<? super Throwable> errorHandler) {
    this.fulfillment = fulfillment;
    this.executionSupplier = executionSupplier;
    this.errorHandler = errorHandler;
  }

  private class Job {
    final Fulfiller<? super T> fulfiller;
    final ExecutionBacking.StreamHandle streamHandle;

    private Job(Fulfiller<? super T> fulfiller, ExecutionBacking.StreamHandle streamHandle) {
      this.fulfiller = fulfiller;
      this.streamHandle = streamHandle;
    }
  }

  @Override
  public SuccessPromise<T> onError(Action<? super Throwable> errorHandler) {
    return new DefaultSuccessPromise<>(executionSupplier, new Fulfillment(), errorHandler);
  }

  @Override
  public void then(Action<? super T> then) {
    newPromise().then(then);
  }

  private DefaultSuccessPromise<T> newPromise() {
    return new DefaultSuccessPromise<>(executionSupplier, new Fulfillment(), errorHandler);
  }

  @Override
  public <O> Promise<O> map(Function<? super T, ? extends O> transformer) {
    return newPromise().map(transformer);
  }

  @Override
  public <O> Promise<O> blockingMap(Function<? super T, ? extends O> transformer) {
    return newPromise().blockingMap(transformer);
  }

  @Override
  public <O> Promise<O> flatMap(Function<? super T, ? extends Promise<O>> transformer) {
    return newPromise().flatMap(transformer);
  }

  @Override
  public Promise<T> route(Predicate<? super T> predicate, Action<? super T> action) {
    return newPromise().route(predicate, action);
  }

  @Override
  public Promise<T> onNull(NoArgAction action) {
    return newPromise().onNull(action);
  }

  @Override
  public Promise<T> defer(Action<? super Runnable> releaser) {
    return newPromise().defer(releaser);
  }

  @Override
  public Promise<T> onYield(Runnable onYield) {
    return newPromise().onYield(onYield);
  }

  @Override
  public Promise<T> wiretap(Action<? super Result<T>> listener) {
    return newPromise().wiretap(listener);
  }

  @Override
  public Promise<T> throttled(Throttle throttle) {
    return newPromise().throttled(throttle);
  }

  @Override
  public Promise<T> cache() {
    return this;
  }

  private void tryDrain() {
    if (draining.compareAndSet(false, true)) {
      try {
        Result<T> result = this.result.get();

        Job job = waiting.poll();
        while (job != null) {
          Job finalJob = job;
          job.streamHandle.complete(e -> finalJob.fulfiller.accept(result));
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

  private class Fulfillment implements Consumer<Fulfiller<? super T>> {

    @Override
    public void accept(Fulfiller<? super T> fulfiller) {
      if (fired.compareAndSet(false, true)) {
        fulfillment.accept(new Fulfiller<T>() {
          @Override
          public void error(Throwable throwable) {
            result.set(Result.<T>failure(throwable));
            fulfiller.error(throwable);
            executionSupplier.get().getEventLoop().execute(CachingPromise.this::tryDrain);
          }

          @Override
          public void success(T value) {
            result.set(Result.success(value));
            fulfiller.success(value);
            executionSupplier.get().getEventLoop().execute(CachingPromise.this::tryDrain);
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

}
