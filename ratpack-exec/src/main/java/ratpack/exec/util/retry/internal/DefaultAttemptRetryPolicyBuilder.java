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

import ratpack.exec.util.retry.AttemptRetryPolicy;
import ratpack.exec.util.retry.AttemptRetryPolicyBuilder;
import ratpack.exec.util.retry.Delay;

public class DefaultAttemptRetryPolicyBuilder implements AttemptRetryPolicyBuilder {

  private Delay delay = DEFAULT_DELAY;
  private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

  @Override
  public AttemptRetryPolicyBuilder delay(Delay delay) {
      this.delay = delay;
      return this;
  }

  @Override
  public AttemptRetryPolicyBuilder maxAttempts(int maxAttempts) {
      this.maxAttempts = maxAttempts;
      return this;
  }

  @Override
  public AttemptRetryPolicy build() {
        return new AttemptRetryPolicy(delay, maxAttempts);
    }
}