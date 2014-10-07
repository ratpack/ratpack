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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.exec.Fulfiller;
import ratpack.exec.OverlappingExecutionException;
import ratpack.func.Action;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SafeFulfiller<T> implements Fulfiller<T> {

  private final static Logger LOGGER = LoggerFactory.getLogger(SafeFulfiller.class);

  private final Fulfiller<? super T> delegate;
  private final AtomicBoolean fulfilled = new AtomicBoolean();
  private final ExecutionBacking executionBacking;

  public SafeFulfiller(ExecutionBacking executionBacking, Fulfiller<? super T> delegate) {
    this.executionBacking = executionBacking;
    this.delegate = delegate;
  }

  public static <T> Consumer<? super Fulfiller<? super T>> wrapping(Supplier<? extends ExecutionBacking> backingSupplier, Action<? super Fulfiller<T>> action) {
    return f -> {
      ExecutionBacking backing = backingSupplier.get();
      SafeFulfiller<T> safe = new SafeFulfiller<>(backing, f);
      backing.continueVia(() -> {
        try {
          action.execute(safe);
        } catch (Throwable throwable) {
          if (!safe.fulfilled.compareAndSet(false, true)) {
            LOGGER.error("", new OverlappingExecutionException("exception thrown after promise was fulfilled", throwable));
          } else {
            f.error(throwable);
          }
        }
      });
    };
  }

  @Override
  public void error(Throwable throwable) {
    if (!fulfilled.compareAndSet(false, true)) {
      LOGGER.error("", new OverlappingExecutionException("promise already fulfilled", throwable));
      return;
    }

    executionBacking.join(e -> delegate.error(throwable));
  }

  @Override
  public void success(T value) {
    if (!fulfilled.compareAndSet(false, true)) {
      LOGGER.error("", new OverlappingExecutionException("promise already fulfilled"));
      return;
    }

    executionBacking.join(e -> delegate.success(value));
  }
}
