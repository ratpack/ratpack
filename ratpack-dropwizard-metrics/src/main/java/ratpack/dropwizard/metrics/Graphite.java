/*
 * Copyright 2013 the original author or authors.
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

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.graphite.GraphiteSender;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * A Configuration implementation to setup {@link GraphiteReporter} instances
 */
public class Graphite {
  private Duration reporterInterval = DropwizardMetricsModule.Config.DEFAULT_INTERVAL;
  private GraphiteSender sender;
  private boolean enabled = true;

  private String includeFilter;
  private String excludeFilter;
  private String prefix;
  private TimeUnit rateUnit;
  private TimeUnit durationUnit;

  /**
   * The state of the Graphite publisher.
   *
   * @return the state of the Graphite publisher
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Enable the Graphite publisher.
   *
   * @return this
   */
  public Graphite enable() {
    this.enabled = true;
    return this;
  }

  /**
   * Disable the Graphite publisher.
   *
   * @return this
   */
  public Graphite disable() {
    this.enabled = false;
    return this;
  }

  /**
   * The interval between metrics reports.
   *
   * @return the interval between metrics reports
   */
  public Duration getReporterInterval() {
    return reporterInterval;
  }

  /**
   * Configure the interval between broadcasts.
   *
   * @param reporterInterval the report interval
   * @return {@code this}
   */
  public Graphite reporterInterval(Duration reporterInterval) {
    this.reporterInterval = reporterInterval;
    return this;
  }

  /**
   * The {@link GraphiteSender} instance.
   *
   * @return the Graphite report sender
   */
  public GraphiteSender getSender() {
    return sender;
  }

  /**
   * Configure the {@link GraphiteSender} instance.
   *
   * @param sender the report sender
   * @return {@code this}
   */
  public Graphite sender(GraphiteSender sender) {
    this.sender = sender;
    return this;
  }

  /**
   * The state of the inclusion metric filter
   *
   * @return the inclusion filter
   */
  public String getIncludeFilter() {
    return includeFilter;
  }

  /**
   * Only report metrics which match the given filter.
   *
   * @param includeFilter a {@link MetricFilter}
   * @return {@code this}
   */
  public Graphite includeFilter(String includeFilter) {
    this.includeFilter = includeFilter;
    return this;
  }

  /**
   * The state of the exclusion metric filter
   *
   * @return the exclusion filter
   */
  public String getExcludeFilter() {
    return excludeFilter;
  }

  /**
   * Do not report metrics which match the given filter.
   *
   * @param excludeFilter a {@link MetricFilter}
   * @return {@code this}
   */
  public Graphite excludeFilter(String excludeFilter) {
    this.excludeFilter = excludeFilter;
    return this;
  }

  /**
   * The state of the metric name prefix
   *
   * @return the metric prefix value
   */
  public String getPrefix() {
    return prefix;
  }

  /**
   * Prefix all metric names with the given string.
   *
   * @param prefix the prefix for all metric names
   * @return {@code this}
   */
  public Graphite prefix(String prefix) {
    this.prefix = prefix;
    return this;
  }

  /**
   * The state of rate conversion
   *
   * @return the rate conversion unit
   */
  public TimeUnit getRateUnit() {
    return rateUnit;
  }

  /**
   * Convert rates to the given time unit.
   *
   * @param rateUnit a unit of time
   * @return {@code this}
   */
  public Graphite rateUnit(TimeUnit rateUnit) {
    this.rateUnit = rateUnit;
    return this;
  }

  /**
   * The state of duration conversion
   *
   * @return the duration conversion unit
   */
  public TimeUnit getDurationUnit() {
    return durationUnit;
  }

  /**
   * Convert durations to the given time unit.
   *
   * @param durationUnit a unit of time
   * @return {@code this}
   */
  public Graphite durationUnit(TimeUnit durationUnit) {
    this.durationUnit = durationUnit;
    return this;
  }
}
