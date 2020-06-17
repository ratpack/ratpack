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
import ratpack.exec.util.retry.internal.DefaultAttemptRetryPolicyBuilder;
import ratpack.exec.func.Action;

import java.time.Duration;

/**
 * An attempt based implementation of {@link RetryPolicy}.
 * <p>
 * This strategy will signal end of retries when the configurable max of retry attempts is surpassed, 5 by default.
 * That number doesn't include the initial request, meaning it will give up after 6 calls, but only 5 retries.
 *
 * @see AttemptRetryPolicyBuilder
 * @since 1.7
 */
public final class AttemptRetryPolicy implements RetryPolicy {

  private final Delay delay;
  private final int maxAttempts;
  private int attempts = 1;

  public AttemptRetryPolicy(Delay delay, int maxAttempts) {
    this.delay = delay;
    this.maxAttempts = maxAttempts;
  }

  /**
   * Builds a new attempt based retry policy from the given definition.
   * @param definition the attempt based retry policy definition
   * @return an attempt based retry policy
   * @throws Exception any thrown by building the attempt based retry policy
   */
  public static AttemptRetryPolicy of(Action<? super AttemptRetryPolicyBuilder> definition) throws Exception {
    return definition.with(new DefaultAttemptRetryPolicyBuilder()).build();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isExhausted() {
    return attempts > maxAttempts;
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
