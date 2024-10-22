/*
 * Copyright 2024 the original author or authors.
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

import ratpack.exec.Downstream;
import ratpack.exec.OverlappingExecutionException;
import ratpack.exec.Upstream;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public final class DelimitedUpstream<T> implements Upstream<T> {

  private final Upstream<T> upstream;

  public DelimitedUpstream(Upstream<T> upstream) {
    this.upstream = upstream;
  }

  public static <T> Upstream<T> of(Upstream<T> upstream) {
    return upstream instanceof DelimitedUpstream ? (DelimitedUpstream<T>) upstream : new DelimitedUpstream<>(upstream);
  }

  @Override
  public void connect(Downstream<? super T> downstream) {
    DefaultExecution.require().delimit(downstream::error, continuation -> {
      Downstream<T> delimitedDownstream = new DelimitedDownstream<>(continuation, downstream);
      try {
        upstream.connect(delimitedDownstream);
      } catch (Throwable throwable) {
        delimitedDownstream.error(throwable);
      }
    });
  }

  private static class DelimitedDownstream<T> implements Downstream<T> {

    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<DelimitedDownstream> FIRED_UPDATER = AtomicIntegerFieldUpdater.newUpdater(DelimitedDownstream.class, "fired");

    @SuppressWarnings("unused")
    volatile int fired;

    private final Continuation continuation;
    private final Downstream<? super T> downstream;

    DelimitedDownstream(Continuation continuation, Downstream<? super T> downstream) {
      this.continuation = continuation;
      this.downstream = downstream;
    }

    boolean fire() {
      return FIRED_UPDATER.compareAndSet(this, 0, 1);
    }

    @Override
    public void error(Throwable throwable) {
      if (fire()) {
        continuation.resume(() -> downstream.error(throwable));
      } else {
        DefaultExecution.LOGGER.error("", new OverlappingExecutionException("promise already fulfilled", throwable));
      }
    }

    @Override
    public void success(T value) {
      if (fire()) {
        continuation.resume(() -> downstream.success(value));
      } else {
        DefaultExecution.LOGGER.error("", new OverlappingExecutionException("promise already fulfilled"));
      }
    }

    @Override
    public void complete() {
      if (fire()) {
        continuation.resume(downstream::complete);
      } else {
        DefaultExecution.LOGGER.error("", new OverlappingExecutionException("promise already fulfilled"));
      }
    }
  }
}
