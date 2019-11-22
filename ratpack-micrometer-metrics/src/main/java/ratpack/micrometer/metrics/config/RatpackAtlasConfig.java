package ratpack.micrometer.metrics.config;

import java.time.Duration;

public class RatpackAtlasConfig extends RatpackStepRegistryConfig<RatpackAtlasConfig> {
  /**
   * URI of the Atlas server.
   */
  private String uri = "http://localhost:7101/api/v1/publish";

  /**
   * Time to live for meters that do not have any activity. After this period the meter
   * will be considered expired and will not get reported.
   */
  private Duration meterTimeToLive = Duration.ofMinutes(15);

  /**
   * Whether to enable streaming to Atlas LWC.
   */
  private boolean lwcEnabled;

  /**
   * Frequency for refreshing config tings from the LWC service.
   */
  private Duration configRefreshFrequency = Duration.ofSeconds(10);

  /**
   * Time to live for subscriptions from the LWC service.
   */
  private Duration configTimeToLive = Duration.ofSeconds(150);

  /**
   * URI for the Atlas LWC endpoint to retrieve current subscriptions.
   */
  private String configUri = "http://localhost:7101/lwc/api/v1/expressions/local-dev";

  /**
   * URI for the Atlas LWC endpoint to evaluate the data for a subscription.
   */
  private String evalUri = "http://localhost:7101/lwc/api/v1/evaluate";

  public String getUri() {
    return this.uri;
  }

  public RatpackAtlasConfig uri(String uri) {
    this.uri = uri;
    return this;
  }

  public Duration getMeterTimeToLive() {
    return this.meterTimeToLive;
  }

  public RatpackAtlasConfig meterTimeToLive(Duration meterTimeToLive) {
    this.meterTimeToLive = meterTimeToLive;
    return this;
  }

  public boolean isLwcEnabled() {
    return this.lwcEnabled;
  }

  public RatpackAtlasConfig lwcEnabled(boolean lwcEnabled) {
    this.lwcEnabled = lwcEnabled;
    return this;
  }

  public Duration getConfigRefreshFrequency() {
    return this.configRefreshFrequency;
  }

  public RatpackAtlasConfig configRefreshFrequency(Duration configRefreshFrequency) {
    this.configRefreshFrequency = configRefreshFrequency;
    return this;
  }

  public Duration getConfigTimeToLive() {
    return this.configTimeToLive;
  }

  public RatpackAtlasConfig configTimeToLive(Duration configTimeToLive) {
    this.configTimeToLive = configTimeToLive;
    return this;
  }

  public String getConfigUri() {
    return this.configUri;
  }

  public RatpackAtlasConfig configUri(String configUri) {
    this.configUri = configUri;
    return this;
  }

  public String getEvalUri() {
    return this.evalUri;
  }

  public RatpackAtlasConfig evalUri(String evalUri) {
    this.evalUri = evalUri;
    return this;
  }
}
