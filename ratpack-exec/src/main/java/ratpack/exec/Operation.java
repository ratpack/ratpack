/*
 * Copyright 2015 the original author or authors.
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

package ratpack.exec;

import ratpack.api.NonBlocking;
import ratpack.exec.internal.DefaultExecution;
import ratpack.exec.internal.DefaultOperation;
import ratpack.exec.internal.DelimitedUpstream;
import ratpack.func.*;

import java.util.Optional;

/**
 * A logical operation.
 * <p>
 * An operation encapsulates a logical piece of work, which will complete some time in the future.
 * It is similar to a {@link Promise} except that it does not produce a value.
 * It merely succeeds, or throws an exception.
 * <p>
 * The {@link #then(Block)} method allows specifying what should happen after the operation completes.
 * The {@link #onError(Action)} method allows specifying what should happen if the operation fails.
 * Like {@link Promise}, the operation will not start until it is subscribed to, via {@link #then(Block)} or {@link #then()}.
 * <p>
 * It is common for methods that would naturally return {@code void} to return an {@link Operation} instead,
 * to allow the method implementation to be effectively asynchronous.
 * The caller of the method is then expected to use the {@link #then(Block)} method to specify what should happen after the operation
 * that the method represents finishes.
 * <pre class="java">{@code
 * import ratpack.exec.Blocking;
 * import ratpack.exec.Operation;
 * import com.google.common.collect.Lists;
 * import ratpack.test.exec.ExecHarness;
 *
 * import java.util.Arrays;
 * import java.util.List;
 *
 * import static org.junit.Assert.assertEquals;
 *
 * public class Example {
 *   public static void main(String... args) throws Exception {
 *     List<String> events = Lists.newArrayList();
 *     ExecHarness.runSingle(e ->
 *       Operation.of(() ->
 *         Blocking.get(() -> events.add("1"))
 *           .then(b -> events.add("2"))
 *       )
 *       .then(() -> events.add("3"))
 *     );
 *     assertEquals(Arrays.asList("1", "2", "3"), events);
 *   }
 * }
 * }</pre>
 */
@NonBlocking
public interface Operation extends Upstream<Void> {

  static Operation async(Upstream<Void> upstream) {
    if (upstream instanceof Operation) {
      return (Operation) upstream;
    } else {
      return DefaultOperation.of(DelimitedUpstream.of(upstream));
    }
  }

  static Operation of(Block block) {
    return DefaultOperation.of(down ->
      DefaultExecution.require().delimit(down::error, continuation -> {
        block.execute();
        continuation.resume(() -> down.success(null));
      })
    );
  }

  static Operation noop() {
    return of(Block.noop());
  }

  /**
   * Create an operation that delegates to another operation.
   *
   * @param factory a factory for the operation
   * @return an operation
   * @since 1.5
   */
  static Operation flatten(Factory<Operation> factory) {
    return DefaultOperation.of(down -> {
      Operation operation;
      try {
        operation = factory.create();
      } catch (Throwable t) {
        down.error(t);
        return;
      }

      operation.connect(down);
    });
  }

  /**
   * Create an operation that fails with the given throwable
   *
   * @param error the error
   * @return an operation
   * @since 1.10
   */
  static Operation error(Throwable error) {
    return DefaultOperation.of(down -> down.error(error));
  }

  default Operation onError(Action<? super Throwable> onError) {
    return onError(Predicate.TRUE, onError);
  }

  /**
   * Specifies the action to take if the an error occurs performing the operation that the given predicate applies to.
   * <p>
   * If the given action throws an exception, the original exception will be rethrown with the exception thrown
   * by the action added to the suppressed exceptions list.
   *
   * @param predicate the predicate to test against the error
   * @param errorHandler the action to take if an error occurs
   * @return An operation for the successful result
   * @since 1.9
   */
  default Operation onError(Predicate<? super Throwable> predicate, Action<? super Throwable> errorHandler) {
    return transform(up -> down -> up.connect(down.consumeErrorIf(predicate, errorHandler)));
  }

  /**
   * Specifies the action to take if the an error of the given type occurs trying to perform the operation.
   * <p>
   * If the given action throws an exception, the original exception will be rethrown with the exception thrown
   * by the action added to the suppressed exceptions list.
   *
   * @param errorType the type of exception to handle with the given action
   * @param errorHandler the action to take if an error occurs
   * @param <E> the type of exception to handle with the given action
   * @return An operation for the successful result
   * @since 1.9
   */
  default <E extends Throwable> Operation onError(Class<E> errorType, Action<? super E> errorHandler) {
    return onError(errorType::isInstance, t -> errorHandler.execute(errorType.cast(t)));
  }

  /**
   * Convert an error to a success or different error.
   * <p>
   * The given action receives the upstream error and is executed as an operation.
   * If the operation completes without error, the original error is considered handled
   * and the returned operation will propagate success.
   * <p>
   * If the given action operation throws an exception,
   * the returned operation will propagate that exception.
   * <p>
   * This method differs to {@link #onError(Action)} in that it does not terminate the operation.
   *
   * @param action the error handler
   * @return an operation
   * @since 1.5
   */
  default Operation mapError(@NonBlocking Action<? super Throwable> action) {
    return transform(up -> down ->
      up.connect(down.onError(throwable ->
        Operation.of(() -> action.execute(throwable))
          .connect(down)
      ))
    );
  }

  /**
   * Registers a listener that is invoked when the operation is initiated.
   *
   * @param onYield the action to take when the operation is initiated
   * @return the operation
   * @since 1.10
   */
  default Operation onYield(Runnable onYield) {
    return transform(up -> down -> {
      try {
        onYield.run();
      } catch (Throwable e) {
        down.error(e);
        return;
      }
      up.connect(down);
    });
  }

  /**
   * Facilitates intercepting an operation before and after.
   *
   * @param before the before listener
   * @param after the after listener
   * @return an operation
   * @since 1.10
   */
  default Operation around(Runnable before, Function<? super ExecResult<Void>, ? extends ExecResult<Void>> after) {
    return transform(up -> down -> {
      try {
        before.run();
      } catch (Throwable e) {
        down.error(e);
        return;
      }

      up.connect(new Downstream<Void>() {
        private void onResult(ExecResult<Void> originalResult) {
          ExecResult<Void> newResult;
          try {
            newResult = after.apply(originalResult);
          } catch (Throwable t) {
            down.error(t);
            return;
          }

          down.accept(newResult);
        }

        @Override
        public void success(Void value) {
          onResult(ExecResult.of(Result.success(value)));
        }

        @Override
        public void error(Throwable throwable) {
          onResult(ExecResult.of(Result.error(throwable)));
        }

        @Override
        public void complete() {
          onResult(ExecResult.complete());
        }
      });
    });
  }

  /**
   * Facilitates executing something before a promise is yielded and after it has completed.
   *
   * @param before the before block
   * @param after the after block
   * @return an operation
   * @since 1.10
   */
  default Operation around(Block before, Block after) {
    return transform(up -> down -> {
      try {
        before.execute();
      } catch (Throwable e) {
        down.error(e);
        return;
      }

      up.connect(new Downstream<Void>() {
        @Override
        public void success(Void value) {
          try {
            after.execute();
          } catch (Throwable e) {
            down.error(e);
            return;
          }
          down.success(value);
        }

        @Override
        public void error(Throwable throwable) {
          try {
            after.execute();
          } catch (Throwable e) {
            if (e != throwable) {
              throwable.addSuppressed(e);
            }
          }
          down.error(throwable);
        }

        @Override
        public void complete() {
          try {
            after.execute();
          } catch (Throwable e) {
            down.error(e);
            return;
          }
          down.complete();
        }
      });
    });
  }

  /**
   * Throttles the operation using the given {@link Throttle throttle}.
   * <p>
   * Throttling can be used to limit concurrency.
   * Typically, to limit concurrent use of an external resource, such as a HTTP API.
   *
   * @param throttle the particular throttle to use to throttle the operation
   * @return the throttled operation
   * @since 1.10
   */
  default Operation throttled(Throttle throttle) {
    return throttle.throttle(this);
  }

  /**
   * Consume the operation as a {@link Result}.
   *
   * @param resultHandler the consumer of the result
   * @since 1.10
   */
  default void result(Action<? super ExecResult<Void>> resultHandler) {
    connect(new Downstream<Void>() {
      @Override
      public void success(Void value) {
        try {
          resultHandler.execute(ExecResult.of(Result.success(value)));
        } catch (Throwable e) {
          DefaultExecution.throwError(e);
        }
      }

      @Override
      public void error(Throwable throwable) {
        try {
          resultHandler.execute(ExecResult.of(Result.error(throwable)));
        } catch (Throwable e) {
          DefaultExecution.throwError(e);
        }
      }

      @Override
      public void complete() {
        try {
          resultHandler.execute(ExecResult.complete());
        } catch (Throwable e) {
          DefaultExecution.throwError(e);
        }
      }
    });
  }

  /**
   * Create a promise for an exec result of this promise.
   *
   * @since 1.10
   */
  default Promise<ExecResult<Void>> result() {
    return Promise.async(down -> connect(new Downstream<Void>() {
      @Override
      public void success(Void value) {
        down.success(ExecResult.of(Result.success(value)));
      }

      @Override
      public void error(Throwable throwable) {
        down.success(ExecResult.of(Result.error(throwable)));
      }

      @Override
      public void complete() {
        down.success(ExecResult.complete());
      }
    }));
  }

  void then(Block block);

  default void then() {
    then(Block.noop());
  }

  Promise<Void> promise();

  default <T> Promise<T> map(Factory<? extends T> factory) {
    return Promise.async(down ->
      connect(down.onSuccess(ignored -> {
        T t;
        try {
          t = factory.create();
        } catch (Throwable throwable) {
          down.error(throwable);
          return;
        }
        down.success(t);
      }))
    );
  }

  default <T> Promise<T> flatMap(Factory<? extends Promise<T>> factory) {
    return Promise.async(down ->
      connect(down.onSuccess(ignored -> {
        Promise<T> t;
        try {
          t = factory.create();
        } catch (Throwable throwable) {
          down.error(throwable);
          return;
        }
        t.connect(down);
      }))
    );
  }

  default <T> Promise<T> flatMap(Promise<T> promise) {
    return Promise.async(down -> connect(down.onSuccess(ignored -> promise.connect(down))));
  }

  default Operation next(Operation operation) {
    return transform(up -> down -> up.connect(down.onSuccess(ignored -> operation.connect(down))));
  }

  default Operation next(Block operation) {
    return next(Operation.of(operation));
  }

  /**
   * Executes the given block as an operation, on a blocking thread.
   *
   * @param operation a block of code to be executed, on a blocking thread
   * @return an operation
   * @since 1.4
   */
  default Operation blockingNext(Block operation) {
    return next(Blocking.op(operation));
  }

  default <O> O to(Function<? super Operation, ? extends O> function) throws Exception {
    return function.apply(this);
  }

  /**
   * Listens for the operation outcome.
   *
   * @param result an empty optional if the operation succeeds, or an optional of the operation error.
   * @return an operation
   * @since 1.6
   */
  default Operation wiretap(Action<? super Optional<? extends Throwable>> result) {
    return transform(up -> down -> up.connect(new Downstream<Void>() {
      @Override
      public void success(Void value) {
        notify(Optional.empty());
      }

      @Override
      public void error(Throwable throwable) {
        notify(Optional.of(throwable));
      }

      private void notify(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Throwable> signal) {
        try {
          result.execute(signal);
        } catch (Throwable t) {
          down.error(t);
          return;
        }

        if (signal.isPresent()) {
          down.error(signal.get());
        } else {
          down.success(null);
        }
      }

      @Override
      public void complete() {
        down.complete();
      }
    }));
  }

  Operation transform(Function<? super Upstream<Void>, ? extends Upstream<Void>> upstreamTransformer);

  /**
   * A low level hook for consuming the promised value.
   * <p>
   * It is generally preferable to use {@link #then()} over this method.
   *
   * @param downstream the downstream consumer
   * @since 1.10
   */
  void connect(Downstream<? super Void> downstream);

  /**
   * Closes the given closeable control flows to this point.
   * <p>
   * This can be used to simulate a try/finally synchronous construct.
   * It is typically used to close some resource after an asynchronous operation.
   * <p>
   * The general pattern is to open the resource, and then pass it to some method/closure that works with it and returns an operation.
   * This method is then called on the returned operation to clean up the resource.
   *
   * @param closeable the closeable to close
   * @return an operation
   * @see #close(Operation)
   * @since 1.10
   */
  default Operation close(AutoCloseable closeable) {
    return transform(up -> down -> up.connect(down.closing(closeable)));
  }

  /**
   * Like {@link #close(AutoCloseable)}, but allows async close operations.
   *
   * @param closer the close operation.
   * @return an operation
   * @since 1.10
   */
  default Operation close(Operation closer) {
    return transform(up -> down -> up.connect(down.closing(closer)));
  }


}
