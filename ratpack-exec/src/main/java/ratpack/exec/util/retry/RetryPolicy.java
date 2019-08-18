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

/**
 * A strategy object to govern retry behaviours.
 * <p>
 * Implementors should define their own exhaustion policy, i.e. when they will give up retrying.
 * Implementors should track retry attempts, even if they don't leverage it when deciding exhaustion.
 * Exampled of other uses are logging or to customize delays.
 * Implementors should also define what kind of delay will be executed between retries.
 * @see Delay and its implementors for different strategies.
 *
 * <pre class="java">{@code
 * import ratpack.exec.ExecResult;
 * import ratpack.exec.Promise;
 * import ratpack.exec.util.retry.AttemptRetryPolicy;
 * import ratpack.exec.util.retry.RetryPolicy;
 * import ratpack.exec.util.retry.FixedDelay;
 * import ratpack.test.exec.ExecHarness;
 *
 * import java.time.Duration;
 * import java.util.Arrays;
 * import java.util.LinkedList;
 * import java.util.List;
 * import java.util.concurrent.atomic.AtomicInteger;
 *
 * import static org.junit.Assert.assertEquals;
 *
 * public class Example {
 *   private static final List<String> LOG = new LinkedList<>();
 *
 *   public static void main(String... args) throws Exception {
 *     AtomicInteger source = new AtomicInteger();
 *
 *     RetryPolicy retryPolicy = AttemptRetryPolicy.of(b -> b
 *       .delay(FixedDelay.of(Duration.ofMillis(500)))
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
 * @see Promise#retry(RetryPolicy, ratpack.func.BiAction)
 * @since 1.7
 */
public interface RetryPolicy {

  /**
   * If the caller should stop retrying.
   * @return TRUE if the caller should stop retrying
   */
  boolean isExhausted();

  /**
   * Attempts performed so far. Starts on 1, i.e. when no retry has been performed yet this returns 1.
   * @return attempts performed so far.
   */
  int attempts();

  /**
   * Increase number of attempts.
   * @return this policy after updating the internal state around attempts
   */
  RetryPolicy increaseAttempt();

  /**
   * Promise that returns the waiting time before retrying.
   * @return promise that returns the waiting time before retrying
   * @see Delay and its implementors for different strategies
   */
  Promise<Duration> delay();

}
