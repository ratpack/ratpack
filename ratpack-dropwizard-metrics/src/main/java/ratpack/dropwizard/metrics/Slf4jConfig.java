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

import com.codahale.metrics.Slf4jReporter;
import org.slf4j.Logger;
import org.slf4j.Marker;

import java.util.concurrent.TimeUnit;

public class Slf4jConfig extends ScheduledReporterConfigSupport<Slf4jConfig> {
  private boolean enabled = true;
  private Marker marker;
  private String prefix;
  private TimeUnit durationUnit;
  private TimeUnit rateUnit;
  private Slf4jReporter.LoggingLevel logLevel;
  private Logger logger;

  /**
   * The state of the marker.
   *
   * @return the marker instance
   */
  public Marker getMarker() {
    return this.marker;
  }

  /**
   * Mark all logged metrics with the given marker.
   *
   * @param marker an SLF4J {@link Marker}
   * @return {@code this}
   */
  public Slf4jConfig marker(Marker marker) {
    this.marker = marker;
    return this;
  }

  /**
   * The logger prefix.
   *
   * @return the prefix text
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
  public Slf4jConfig prefix(String prefix) {
    this.prefix = prefix;
    return this;
  }

  /**
   * The state of the duration time unit.
   *
   * @return the duration unit instance
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
  public Slf4jConfig durationUnit(TimeUnit durationUnit) {
    this.durationUnit = durationUnit;
    return this;
  }

  /**
   * The state of the rate time unit.
   *
   * @return the rate unit instance
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
  public Slf4jConfig rateUnit(TimeUnit rateUnit) {
    this.rateUnit = rateUnit;
    return this;
  }

  /**
   * The state of the logging level.
   *
   * @return the log level instance
   */
  public Slf4jReporter.LoggingLevel getLogLevel() {
    return this.logLevel;
  }

  /**
   * Use Logging Level when reporting.
   *
   * @param logLevel a (@link LoggingLevel}
   * @return {@code this}
   */
  public Slf4jConfig logLevel(Slf4jReporter.LoggingLevel logLevel) {
    this.logLevel = logLevel;
    return this;
  }

  /**
   * The state of the logger.
   *
   * @return the logger instance
   */
  public Logger getLogger() {
    return this.logger;
  }

  /**
   * Log metrics to the given logger.
   *
   * @param logger an SLF4J {@link Logger}
   * @return {@code this}
   */
  public Slf4jConfig logger(Logger logger) {
    this.logger = logger;
    return this;
  }

  /**
   * The state of the Slf4j publisher.
   *
   * @return the state of the Console publisher
   */
  public boolean isEnabled() {
    return this.enabled;
  }

  /**
   * Set the state of the Slf4j publisher.
   *
   * @param enabled True if metrics are published to the logger. False otherwise
   * @return {@code this}
   */
  public Slf4jConfig enable(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

}
