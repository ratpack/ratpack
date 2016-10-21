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

package ratpack.exec.util;

import ratpack.exec.Downstream;
import ratpack.exec.ExecResult;
import ratpack.exec.Promise;
import ratpack.exec.Result;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A logical value that will be available later, that promises can be created for.
 * <p>
 * A “promised” can be used by the producer of a value to notify an interested parties when the value becomes available.
 * Zero or more promises can be created for the promised value via the {@link #promise()} method.
 * {@code Promised} extends {@link Downstream}, which represents the <em>write</em> side of async values.
 * To supply the promised value, simply call <b>exactly one</b> of the methods inherited from {@link Downstream}.
 * <p>
 * A “promised” can have zero or more listeners, but can only be fulfilled once.
 * All of the methods inherited from {@link Downstream} will throw an {@link AlreadySuppliedException}
 * if a value, error or completion have already been signalled for this topic.
 *
 * <pre class="java">{@code
 * import ratpack.test.exec.ExecHarness;
 * import ratpack.exec.Execution;
 * import ratpack.exec.util.Promised;
 *
 * import java.util.concurrent.atomic.AtomicReference;
 * import java.util.concurrent.CountDownLatch;
 *
 * import static org.junit.Assert.assertEquals;
 *
 * public class Example {
 *   public static void main(String... args) throws Exception {
 *     AtomicReference<Long> ref = new AtomicReference<>();
 *     CountDownLatch latch = new CountDownLatch(1);
 *
 *     ExecHarness.runSingle(e -> {
 *       Promised<Long> topic = new Promised<>();
 *
 *       // create a listener
 *       Execution.fork().start(e1 ->
 *         topic.promise().then(v -> {
 *           ref.set(v);
 *           latch.countDown();
 *         })
 *       );
 *
 *       // fulfill the topic, notifying listeners
 *       topic.success(1l);
 *     });
 *
 *     latch.await();
 *     assertEquals(1l, ref.get().longValue());
 *   }
 * }
 * }</pre>
 *
 * @param <T> the type of value that is promised
 * @since 1.2
 */
public final class Promised<T> implements Downstream<T> {

  private final AtomicReference<ExecResult<? extends T>> ref = new AtomicReference<>();
  private final Queue<Downstream<? super T>> listeners = new ConcurrentLinkedDeque<>();

  /**
   * Creates a new promise for the eventual value.
   *
   * @return a new promise for the eventual value
   */
  public Promise<T> promise() {
    return Promise.async(downstream -> {
      ExecResult<? extends T> result = ref.get();
      if (result == null) {
        listeners.add(downstream);
        drain();
      } else {
        downstream.accept(result);
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void success(T value) {
    accept(ExecResult.of(Result.success(value)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void error(Throwable throwable) {
    accept(ExecResult.of(Result.error(throwable)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void complete() {
    accept(ExecResult.complete());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void accept(ExecResult<? extends T> result) {
    if (ref.compareAndSet(null, result)) {
      drain();
    } else {
      throw new AlreadySuppliedException("promised has already been completed with " + ref.get());
    }
  }

  private void drain() {
    ExecResult<? extends T> result = ref.get();
    if (result == null) {
      return;
    }

    Downstream<? super T> next = listeners.poll();
    while (next != null) {
      next.accept(result);
      next = listeners.poll();
    }
  }

  /**
   * Thrown if an attempt is made to supply the value/result after it has already been supplied.
   */
  public static class AlreadySuppliedException extends IllegalStateException {
    private AlreadySuppliedException(String s) {
      super(s);
    }
  }
}
