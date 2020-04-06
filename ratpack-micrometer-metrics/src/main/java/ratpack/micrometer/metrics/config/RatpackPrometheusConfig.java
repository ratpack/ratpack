/*
 * Copyright 2020 the original author or authors.
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

package ratpack.micrometer.metrics.config;

import java.time.Duration;

public class RatpackPrometheusConfig extends RatpackMeterRegistryConfig<RatpackPrometheusConfig> {
  /**
   * Whether to enable publishing descriptions as part of the scrape payload to
   * Prometheus. Turn this off to minimize the amount of data sent on each scrape.
   */
  private boolean descriptions = true;

  /**
   * Step size (i.e. reporting frequency) to use.
   */
  private Duration step = Duration.ofMinutes(1);

  public boolean isDescriptions() {
    return this.descriptions;
  }

  public RatpackPrometheusConfig descriptions(boolean descriptions) {
    this.descriptions = descriptions;
    return this;
  }

  public Duration getStep() {
    return this.step;
  }

  public RatpackPrometheusConfig step(Duration step) {
    this.step = step;
    return this;
  }
}
