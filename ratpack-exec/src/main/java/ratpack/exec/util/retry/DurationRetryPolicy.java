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
import ratpack.exec.util.retry.internal.DefaultDurationRetryPolicyBuilder;
import ratpack.exec.func.Action;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * A duration based implementation of {@link RetryPolicy}.
 * <p>
 * This strategy will signal end of retries when the elapsed time from the first error occurrence surpasses the configurable
 * max duration, 30 seconds by default.
 *
 * <pre class="java">{@code
 * import ratpack.exec.ExecResult;
 * import ratpack.exec.Promise;
 * import ratpack.exec.util.retry.DurationRetryPolicy;
 * import ratpack.exec.util.retry.RetryPolicy;
 * import ratpack.exec.util.retry.FixedDelay;
 * import ratpack.test.exec.ExecHarness;
 * import ratpack.test.internal.time.FixedWindableClock;
 *
 * import java.time.Duration;
 * import java.time.Instant;
 * import java.time.ZoneOffset;
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
 *     FixedWindableClock clock = new FixedWindableClock(Instant.now(), ZoneOffset.UTC);
 *
 *     RetryPolicy retryPolicy = DurationRetryPolicy.of(b -> b
 *       .delay(FixedDelay.of(Duration.ofMillis(500)))
 *       .maxDuration(Duration.ofSeconds(10))
 *       .clock(clock));
 *
 *     RuntimeException e = new RuntimeException("!");
 *
 *     Throwable result = ExecHarness.yieldSingle(exec ->
 *       Promise.sync(source::incrementAndGet)
 *         .mapIf(i -> i == 3, i -> {
 *           clock.windClock(Duration.ofMinutes(5));
 *           return i;
 *          })
 *         .map(i -> { throw new IllegalStateException(); })
 *         .retry(retryPolicy, (i, t) -> LOG.add("retry attempt: " + i))
 *     ).getThrowable();
 *
 *     assertEquals("java.lang.IllegalStateException", result.getClass().getCanonicalName());
 *     assertEquals(Arrays.asList("retry attempt: 1", "retry attempt: 2"), LOG);
 *   }
 * }
 * }</pre>
 *
 * @see DurationRetryPolicyBuilder
 * @since 1.7
 */
public class DurationRetryPolicy implements RetryPolicy {

  private final Delay delay;
  private final Duration maxDuration;
  private Instant start;
  private int attempts = 1;
  private Clock clock;

  public DurationRetryPolicy(Delay delay, Duration maxDuration, Clock clock) {
    this.delay = delay;
    this.maxDuration = maxDuration;
    this.clock = clock;
  }

  /**
   * Builds a new duration based retry policy from the given definition.
   * @param definition the duration based retry policy definition
   * @return a duration based retry policy
   * @throws Exception any thrown by building the duration based retry policy
   */
  public static DurationRetryPolicy of(Action<? super DurationRetryPolicyBuilder> definition) throws Exception {
    return definition.with(new DefaultDurationRetryPolicyBuilder()).build();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isExhausted() {
    if (start == null) {
      start = clock.instant();
    }
    Instant limit = start.plusMillis(maxDuration.toMillis());
    Instant now = clock.instant();
    return now.isAfter(limit);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int attempts() {
    return attempts;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Promise<Duration> delay() {
    return delay.delay(attempts);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RetryPolicy increaseAttempt() {
    attempts++;
    return this;
  }

}
