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
import ratpack.exec.internal.DefaultOperation;
import ratpack.func.Action;
import ratpack.func.Block;
import ratpack.func.Factory;
import ratpack.func.Function;

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
public interface Operation {

  static Operation of(Block block) {
    return new DefaultOperation(Promise.sync(() -> {
      block.execute();
      return null;
    }));
  }

  /**
   * Create an operation that delegates to another operation.
   *
   * @param factory a factory for the operation
   * @return an operation
   * @since 1.5
   */
  static Operation flatten(Factory<Operation> factory) {
    return new DefaultOperation(Promise.flatten(() -> factory.create().promise()));
  }

  Operation onError(Action<? super Throwable> onError);

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
  default Operation mapError(Action<? super Throwable> action) {
    return promise().transform(up -> down ->
      up.connect(new Downstream<Void>() {
        @Override
        public void success(Void value) {
          down.success(value);
        }

        @Override
        public void error(Throwable throwable) {
          Operation.of(() -> action.execute(throwable)).promise().connect(new Downstream<Void>() {
            @Override
            public void success(Void value) {
              down.success(value);
            }

            @Override
            public void error(Throwable throwable) {
              down.error(throwable);
            }

            @Override
            public void complete() {
              down.complete();
            }
          });

        }

        @Override
        public void complete() {
          down.complete();
        }
      })
    ).operation();
  }

  @NonBlocking
  void then(Block block);

  @NonBlocking
  default void then() {
    then(Block.noop());
  }

  Promise<Void> promise();

  default <T> Promise<T> map(Factory<? extends T> factory) {
    return promise().map(n -> factory.create());
  }

  default <T> Promise<T> flatMap(Factory<? extends Promise<T>> factory) {
    return promise().flatMap(n -> factory.create());
  }

  default <T> Promise<T> flatMap(Promise<T> promise) {
    return promise().flatMap(n -> promise);
  }

  default Operation next(Operation operation) {
    return new DefaultOperation(flatMap(operation::promise));
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

  default Operation wiretap(Action<? super Optional<? extends Throwable>> action) {
    return promise().wiretap(r -> {
        if (r.isError()) {
          action.execute(Optional.of(r.getThrowable()));
        } else {
          action.execute(Optional.<Throwable>empty());
        }
      }
    ).operation();
  }

  static Operation noop() {
    return of(Block.noop());
  }

}
