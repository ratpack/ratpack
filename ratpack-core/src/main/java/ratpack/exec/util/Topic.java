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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A mechanism for creating one or more promises for values that will become available later.
 * <p>
 * A topic can be used to effectively listen for values, by creating promises that will
 * be fulfilled when the value becomes available later.
 * <p>
 * A topic can have zero or more listeners, but can only be fulfilled once.
 * All of the methods inherited from {@link Downstream} will throw an {@link IllegalStateException}
 * if a value, error or completion have already been signalled for this topic.
 *
 * <pre class="java">{@code
 * import ratpack.test.exec.ExecHarness;
 * import ratpack.exec.Execution;
 * import ratpack.exec.util.Topic;
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
 *       Topic<Long> topic = new Topic<>();
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
 * @param <T>
 * @since 2.1
 */
public final class Topic<T> implements Downstream<T> {

  private final AtomicReference<ExecResult<? extends T>> ref = new AtomicReference<>();
  private final Queue<Downstream<? super T>> listeners = new ConcurrentLinkedQueue<>();

  /**
   * Creates a new promise for the eventual value.
   *
   * @return a new promise for the eventual value
   */
  public Promise<T> promise() {
    return Promise.of(downstream -> {
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
      throw new IllegalStateException("topic has already been completed with " + ref.get());
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
}
