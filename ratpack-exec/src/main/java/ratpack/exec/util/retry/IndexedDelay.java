/*
 * Copyright 2018 the original author or authors.
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

package ratpack.exec.util.retry;

import ratpack.exec.Promise;

import java.time.Duration;
import java.util.function.Function;

/**
 * A index based implementation of {@link Delay}. By being configurable through an injected function, callers can implement customs backoff algorithms.
 *
 * <pre class="java">{@code
 * import ratpack.exec.ExecResult;
 * import ratpack.exec.Promise;
 * import ratpack.exec.util.retry.AttemptRetryPolicy;
 * import ratpack.exec.util.retry.RetryPolicy;
 * import ratpack.exec.util.retry.IndexedDelay;
 * import ratpack.test.exec.ExecHarness;
 *
 * import java.time.Duration;
 * import java.util.Arrays;
 * import java.util.LinkedList;
 * import java.util.List;
 * import java.util.concurrent.atomic.AtomicInteger;
 *
 * import static org.junit.jupiter.api.Assertions.assertEquals;
 *
 * public class Example {
 *   private static final List<String> LOG = new LinkedList<>();
 *
 *   public static void main(String... args) throws Exception {
 *     AtomicInteger source = new AtomicInteger();
 *
 *     RetryPolicy retryPolicy = AttemptRetryPolicy.of(b -> b
 *       .delay(IndexedDelay.of(i -> Duration.ofMillis(500).multipliedBy(i)))
 *       .maxAttempts(3));
 *
 *     ExecResult<Integer> result = ExecHarness.yieldSingle(exec ->
 *       Promise.sync(source::incrementAndGet)
 *         .mapIf(i -> i < 3, i -> { throw new IllegalStateException(); })
 *         .retry(retryPolicy, (i, t) -> LOG.add("retry attempt: " + i))
 *     );
 *
 *     assertEquals(Integer.valueOf(3), result.getValue());
 *     assertEquals(Arrays.asList("retry attempt: 1", "retry attempt: 2"), LOG);
 *   }
 * }
 * }</pre>
 *
 * @since 1.7
 */
public class IndexedDelay implements Delay {

  private Function<? super Integer, Duration> indexedDelay;

  private IndexedDelay(Function<? super Integer, Duration> indexedDelay) {
    this.indexedDelay = indexedDelay;
  }

  /**
   * Builds an index based delay.
   * @param indexedDelay a function expecting a retry attempt and returning the delay duration
   * @return an indexed delay
   */
  public static IndexedDelay of(Function<? super Integer, Duration> indexedDelay) {
    return new IndexedDelay(indexedDelay);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Promise<Duration> delay(Integer attempt) {
    return Promise.value(indexedDelay.apply(attempt));
  }
}
