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

import java.time.Duration;

/**
 * Builds an {@link AttemptRetryPolicy}
 * @see AttemptRetryPolicy#of(ratpack.func.Action)
 * @since 1.7
 */
public interface AttemptRetryPolicyBuilder {

  /**
   * By default, retries will wait 1 second between executions.
   */
  Delay DEFAULT_DELAY = FixedDelay.of(Duration.ofSeconds(1));

  /**
   * By default, this retry policy will give up after the fifth retry attempt.
   */
  int DEFAULT_MAX_ATTEMPTS = 5;

  /**
   * Builds an {@link AttemptRetryPolicy}
   * @return a retry policy
   */
  AttemptRetryPolicy build();

  /**
   * The delay strategy
   * @param delay the delay strategy
   * @return this
   */
  AttemptRetryPolicyBuilder delay(Delay delay);

  /**
   * Maximum number of allowed retry attempts
   * @param maxAttempts maximum number of allowed retry attempts
   * @return this
   */
  AttemptRetryPolicyBuilder maxAttempts(int maxAttempts);

}
