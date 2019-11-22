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
