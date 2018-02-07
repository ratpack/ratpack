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

import io.netty.channel.EventLoop;
import ratpack.exec.internal.DefaultExecution;
import ratpack.exec.internal.DefaultPromise;
import ratpack.exec.internal.ExecThreadBinding;
import ratpack.func.Block;
import ratpack.func.Factory;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Facilitates working with code that blocks (e.g. synchronous IO)
 */
public abstract class Blocking {

  private Blocking() {
  }

  /**
   * Performs a blocking operation on a separate thread, returning a promise for its value.
   * <p>
   * This method should be used to perform blocking IO, or to perform any operation that synchronously waits for something to happen.
   * The given factory function will be executed on a thread from a special pool for such operations (i.e. not a thread from the main compute event loop).
   * <p>
   * The operation should do as little computation as possible.
   * It should just perform the blocking operation and immediately return the result.
   * Performing computation during the operation will degrade performance.
   *
   * @param factory the operation that blocks
   * @param <T> the type of value created by the operation
   * @return a promise for the return value of the given blocking operation
   */
  public static <T> Promise<T> get(Factory<T> factory) {
    return new DefaultPromise<>(downstream -> {
      DefaultExecution execution = DefaultExecution.require();
      EventLoop eventLoop = execution.getEventLoop();
      execution.delimit(downstream::error, continuation ->
        eventLoop.execute(() ->
          CompletableFuture.supplyAsync(
            new Supplier<Result<T>>() {
              Result<T> result;

              @Override
              public Result<T> get() {
                try {
                  execution.bindToThread();
                  intercept(execution, execution.getAllInterceptors().iterator(), () -> {
                    try {
                      result = Result.success(factory.create());
                    } catch (Throwable e) {
                      result = Result.error(e);
                    }
                  });
                  return result;
                } catch (Throwable e) {
                  DefaultExecution.interceptorError(e);
                  return result;
                } finally {
                  execution.unbindFromThread();
                }
              }
            }, execution.getController().getBlockingExecutor()
          ).thenAcceptAsync(v -> continuation.resume(() -> downstream.accept(v)), eventLoop)
        )
      );
    });
  }

  /**
   * Blocks execution waiting for this promise to complete and returns the promised value.
   * <p>
   * This method allows the use of asynchronous API, by synchronous API.
   * This may occur when integrating with other libraries that are not asynchronous.
   * The following example simulates using a library that takes a callback that is expected to produce a value synchronously,
   * but where the production of the value is actually asynchronous.
   *
   * <pre class="java">{@code
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.exec.ExecResult;
   * import ratpack.exec.Blocking;
   * import ratpack.exec.Promise;
   * import ratpack.func.Factory;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   static <T> T produceSync(Factory<? extends T> factory) throws Exception {
   *     return factory.create();
   *   }
   *
   *   public static void main(String... args) throws Exception {
   *     ExecResult<String> result = ExecHarness.yieldSingle(e ->
   *       Blocking.get(() ->
   *         produceSync(() ->
   *           Blocking.on(Promise.value("foo")) // block and wait for the promised value
   *         )
   *       )
   *     );
   *
   *     assertEquals("foo", result.getValue());
   *   }
   * }
   * }</pre>
   *
   * <p>
   * <b>Important:</b> this method can only be used inside a {@link Blocking} function.
   * That is, it can only be used from a Ratpack managed blocking thread.
   * If it is called on a non Ratpack managed blocking thread it will immediately throw an {@link ExecutionException}.
   * <p>
   * When this method is called, the promise will be subscribed to on a compute thread while the blocking thread waits.
   * When the promised value has been produced, and the compute thread segment has completed, the value will be returned
   * allowing execution to continue on the blocking thread.
   * The following example visualises this flow by capturing the sequence of events via an {@link ExecInterceptor}.
   *
   * <pre class="java">{@code
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.exec.Blocking;
   * import ratpack.exec.Promise;
   * import ratpack.exec.ExecResult;
   * import ratpack.exec.ExecInterceptor;
   *
   * import java.util.List;
   * import java.util.ArrayList;
   * import java.util.Arrays;
   *
   * import static org.junit.Assert.assertEquals;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     List<String> events = new ArrayList<>();
   *
   *     ExecHarness.yieldSingle(
   *       r -> r.add(ExecInterceptor.class, (execution, execType, continuation) -> {
   *         events.add(execType + "-start");
   *         try {
   *           continuation.execute();
   *         } finally {
   *           events.add(execType + "-stop");
   *         }
   *       }),
   *       e -> Blocking.get(() -> Blocking.on(Promise.value("foo")))
   *     );
   *
   *     List<String> actualEvents = Arrays.asList(
   *       "COMPUTE-start",
   *       "COMPUTE-stop",
   *         "BLOCKING-start",
   *           "COMPUTE-start",
   *           "COMPUTE-stop",
   *         "BLOCKING-stop",
   *       "COMPUTE-start",
   *       "COMPUTE-stop"
   *     );
   *
   *     assertEquals(actualEvents, events);
   *   }
   * }
   * }</pre>
   *
   * @param promise the promise to block on
   * @param <T> the type of value returned by the promise
   * @return the promised value
   * @throws ExecutionException if not called on a Ratpack managed blocking thread
   * @throws Exception any thrown while producing the value
   */
  public static <T> T on(Promise<T> promise) throws Exception {
    ExecThreadBinding.requireBlockingThread("Blocking.on() can only be used while blocking (i.e. use Blocking.get() first)");
    DefaultExecution execution = DefaultExecution.require();
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Result<T>> resultReference = new AtomicReference<>();

    execution.delimit(t -> {
        resultReference.set(Result.error(t));
        latch.countDown();
      }, continuation ->
        promise.connect(
          new Downstream<T>() {
            @Override
            public void success(T value) {
              unlatch(Result.success(value));
            }

            @Override
            public void error(Throwable throwable) {
              unlatch(Result.error(throwable));
            }

            @Override
            public void complete() {
              unlatch(Result.success(null));
            }

            private void unlatch(Result<T> result) {
              continuation.resume(() ->
                execution.getEventLoop().execute(() -> {
                  resultReference.set(result);
                  latch.countDown();
                })
              );
            }
          }
        )
    );

    execution.eventLoopDrain();
    latch.await();
    return resultReference.get().getValueOrThrow();
  }

  public static Operation op(Block block) {
    return Blocking.<Void>get(() -> {
      block.execute();
      return null;
    }).operation();
  }

  public static void exec(Block block) {
    op(block).then();
  }

  private static void intercept(Execution execution, final Iterator<? extends ExecInterceptor> interceptors, Runnable runnable) throws Exception {
    if (interceptors.hasNext()) {
      interceptors.next().intercept(execution, ExecInterceptor.ExecType.BLOCKING, () -> intercept(execution, interceptors, runnable));
    } else {
      runnable.run();
    }
  }
}
