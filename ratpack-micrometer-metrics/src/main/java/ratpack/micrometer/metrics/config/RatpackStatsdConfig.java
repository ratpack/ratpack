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

public class RatpackStatsdConfig extends RatpackMeterRegistryConfig<RatpackStatsdConfig> {
  /**
   * Whether exporting of metrics to StatsD is enabled.
   */
  private boolean enabled = true;

  /**
   * StatsD line protocol to use. Must be one of etsy, datadog, telegraf, or sysdig
   * (case insensitive).
   */
  private String flavor = "datadog";

  /**
   * Host of the StatsD server to receive exported metrics.
   */
  private String host = "localhost";

  /**
   * Port of the StatsD server to receive exported metrics.
   */
  private Integer port = 8125;

  /**
   * Total length of a single payload should be kept within your network's MTU.
   */
  private Integer maxPacketLength = 1400;

  /**
   * How often gauges will be polled. When a gauge is polled, its value is recalculated
   * and if the value has changed (or publishUnchangedMeters is true), it is sent to the
   * StatsD server.
   */
  private Duration pollingFrequency = Duration.ofSeconds(10);

  /**
   * Whether to send unchanged meters to the StatsD server.
   */
  private boolean publishUnchangedMeters = true;

  public boolean isEnabled() {
    return this.enabled;
  }

  public RatpackStatsdConfig enabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  public String getFlavor() {
    return this.flavor;
  }

  public RatpackStatsdConfig flavor(String flavor) {
    this.flavor = flavor;
    return this;
  }

  public String getHost() {
    return this.host;
  }

  public RatpackStatsdConfig host(String host) {
    this.host = host;
    return this;
  }

  public int getPort() {
    return this.port;
  }

  public RatpackStatsdConfig port(int port) {
    this.port = port;
    return this;
  }

  public int getMaxPacketLength() {
    return this.maxPacketLength;
  }

  public RatpackStatsdConfig maxPacketLength(int maxPacketLength) {
    this.maxPacketLength = maxPacketLength;
    return this;
  }

  public Duration getPollingFrequency() {
    return this.pollingFrequency;
  }

  public RatpackStatsdConfig pollingFrequency(Duration pollingFrequency) {
    this.pollingFrequency = pollingFrequency;
    return this;
  }

  public boolean isPublishUnchangedMeters() {
    return this.publishUnchangedMeters;
  }

  public RatpackStatsdConfig publishUnchangedMeters(boolean publishUnchangedMeters) {
    this.publishUnchangedMeters = publishUnchangedMeters;
    return this;
  }
}
