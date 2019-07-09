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

import java.time.Clock;
import java.time.Duration;

/**
 * Builds a {@link DurationRetryPolicy}
 * @see DurationRetryPolicy#of(ratpack.func.Action)
 * @since 1.7
 */
public interface DurationRetryPolicyBuilder {

  /**
   * By default, retries will wait 1 second between executions.
   */
  Delay DEFAULT_DELAY = FixedDelay.of(Duration.ofSeconds(1));

  /**
   * By default, this retry policy will give up after 30 seconds since the first error occurrence.
   */
  Duration DEFAULT_MAX_DURATION = Duration.ofSeconds(30);

  /**
   * There should be no reasons for changing this on production code.
   */
  Clock DEFAULT_CLOCK = Clock.systemDefaultZone();

  /**
   * Builds a {@link DurationRetryPolicy}
   * @return a retry policy
   */
  DurationRetryPolicy build();

  /**
   * The delay strategy.
   *
   * @param delay the delay strategy
   * @return this
   */
  DurationRetryPolicyBuilder delay(Delay delay);

  /**
   * Maximum duration until timeout of the retry policy.
   *
   * @param maxDuration the maximum duration
   * @return this
   */
  DurationRetryPolicyBuilder maxDuration(Duration maxDuration);

  /**
   * Clock used to determine current time.
   *
   * @param clock clock used to determine current time
   * @return this
   */
  DurationRetryPolicyBuilder clock(Clock clock);

}
