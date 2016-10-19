/*
 * Copyright 2015 the original author or authors.
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

package ratpack.dropwizard.metrics;

import java.time.Duration;

/**
 * A common base for scheduled reporter config classes.
 *
 * @param <T> self type
 * @since 1.4
 */
public abstract class ScheduledReporterConfigSupport<T extends ReporterConfigSupport<T>> extends ReporterConfigSupport<T> {

  private Duration reporterInterval = Duration.ofSeconds(30);

  /**
   * The interval between metrics reports.
   * @return the interval between metrics reports
   */
  public Duration getReporterInterval() {
    return this.reporterInterval;
  }

  /**
   * Configure the interval between metrics reports.
   *
   * @param reporterInterval the report interval
   * @return {@code this}
   */
  @SuppressWarnings("unchecked")
  public T reporterInterval(Duration reporterInterval) {
    this.reporterInterval = reporterInterval;
    return (T)this;
  }

}
