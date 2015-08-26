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

import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.graphite.GraphiteSender;

import java.util.concurrent.TimeUnit;

/**
 * A Configuration implementation to setup {@link GraphiteReporter} instances.
 */
public class GraphiteConfig extends ScheduledReporterConfigSupport<GraphiteConfig> {
  private GraphiteSender sender;
  private boolean enabled = true;
  private String prefix;
  private TimeUnit rateUnit;
  private TimeUnit durationUnit;

  /**
   * The state of the Graphite publisher.
   *
   * @return the state of the Graphite publisher
   */
  public boolean isEnabled() {
    return this.enabled;
  }

  /**
   * Enable the Graphite publisher.
   *
   * @return this
   */
  public GraphiteConfig enable() {
    this.enabled = true;
    return this;
  }

  /**
   * Disable the Graphite publisher.
   *
   * @return this
   */
  public GraphiteConfig disable() {
    this.enabled = false;
    return this;
  }

  /**
   * The {@link GraphiteSender} instance.
   *
   * @return the Graphite report sender
   */
  public GraphiteSender getSender() {
    return this.sender;
  }

  /**
   * Configure the {@link GraphiteSender} instance.
   *
   * @param sender the report sender
   * @return {@code this}
   */
  public GraphiteConfig sender(GraphiteSender sender) {
    this.sender = sender;
    return this;
  }

  /**
   * The state of the metric name prefix.
   *
   * @return the metric prefix value
   */
  public String getPrefix() {
    return this.prefix;
  }

  /**
   * Prefix all metric names with the given string.
   *
   * @param prefix the prefix for all metric names
   * @return {@code this}
   */
  public GraphiteConfig prefix(String prefix) {
    this.prefix = prefix;
    return this;
  }

  /**
   * The state of rate conversion.
   *
   * @return the rate conversion unit
   */
  public TimeUnit getRateUnit() {
    return this.rateUnit;
  }

  /**
   * Convert rates to the given time unit.
   *
   * @param rateUnit a unit of time
   * @return {@code this}
   */
  public GraphiteConfig rateUnit(TimeUnit rateUnit) {
    this.rateUnit = rateUnit;
    return this;
  }

  /**
   * The state of duration conversion.
   *
   * @return the duration conversion unit
   */
  public TimeUnit getDurationUnit() {
    return this.durationUnit;
  }

  /**
   * Convert durations to the given time unit.
   *
   * @param durationUnit a unit of time
   * @return {@code this}
   */
  public GraphiteConfig durationUnit(TimeUnit durationUnit) {
    this.durationUnit = durationUnit;
    return this;
  }

}
