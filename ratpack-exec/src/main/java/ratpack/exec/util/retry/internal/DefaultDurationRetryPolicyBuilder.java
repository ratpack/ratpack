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

package ratpack.exec.util.retry.internal;

import ratpack.exec.util.retry.Delay;
import ratpack.exec.util.retry.DurationRetryPolicy;
import ratpack.exec.util.retry.DurationRetryPolicyBuilder;

import java.time.Clock;
import java.time.Duration;

public class  DefaultDurationRetryPolicyBuilder implements DurationRetryPolicyBuilder {

  private Delay delay = DEFAULT_DELAY;
  private Duration maxDuration = DEFAULT_MAX_DURATION;
  private Clock clock = DEFAULT_CLOCK;

  @Override
  public DurationRetryPolicyBuilder delay(Delay delay) {
      this.delay = delay;
      return this;
  }

  @Override
  public DurationRetryPolicyBuilder maxDuration(Duration maxDuration) {
      this.maxDuration = maxDuration;
      return this;
  }

  @Override
  public DurationRetryPolicyBuilder clock(Clock clock) {
      this.clock = clock;
      return this;
  }

  @Override
  public DurationRetryPolicy build() {
        return new DurationRetryPolicy(delay, maxDuration, clock);
    }
}
