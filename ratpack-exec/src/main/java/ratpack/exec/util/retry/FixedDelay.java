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
 * A fixed duration based implementation of {@link Delay}.
 * @since 1.7
 */
public class FixedDelay implements Delay {

  private final Duration delay;

  private FixedDelay(Duration delay) {
    this.delay = delay;
  }

  /**
   * Builds a fixed duration delay.
   * @param duration the fixed duration
   * @return a fixed delay
   */
  public static FixedDelay of(Duration duration) {
    return new FixedDelay(duration);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Promise<Duration> delay(Integer attempt) {
    return Promise.value(delay);
  }
}
