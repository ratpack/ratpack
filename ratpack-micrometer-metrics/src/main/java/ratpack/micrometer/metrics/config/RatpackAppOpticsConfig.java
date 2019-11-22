package ratpack.micrometer.metrics.config;

import java.time.Duration;

public class RatpackAppOpticsConfig extends RatpackStepRegistryConfig<RatpackAppOpticsConfig> {
  /**
   * URI to ship metrics to.
   */
  private String uri = "https://api.appoptics.com/v1/measurements";

  /**
   * AppOptics API token.
   */
  private String apiToken;

  /**
   * Tag that will be mapped to "@host" when shipping metrics to AppOptics.
   */
  private String hostTag = "instance";

  /**
   * Number of measurements per request to use for this backend. If more measurements
   * are found, then multiple requests will be made.
   */
  private int batchSize = 500;

  /**
   * Connection timeout for requests to this backend.
   */
  private Duration connectTimeout = Duration.ofSeconds(5);

  public String getUri() {
    return this.uri;
  }

  public RatpackAppOpticsConfig uri(String uri) {
    this.uri = uri;
    return this;
  }

  public String getApiToken() {
    return this.apiToken;
  }

  public RatpackAppOpticsConfig apiToken(String apiToken) {
    this.apiToken = apiToken;
    return this;
  }

  public String getHostTag() {
    return this.hostTag;
  }

  public RatpackAppOpticsConfig hostTag(String hostTag) {
    this.hostTag = hostTag;
    return this;
  }

  public int getBatchSize() {
    return this.batchSize;
  }

  public RatpackAppOpticsConfig batchSize(int batchSize) {
    this.batchSize = batchSize;
    return this;
  }
}
