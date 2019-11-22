package ratpack.micrometer.metrics.config;

public class RatpackDynatraceConfig extends RatpackStepRegistryConfig<RatpackDynatraceConfig> {
  /**
   * Dynatrace authentication token.
   */
  private String apiToken;

  /**
   * ID of the custom device that is exporting metrics to Dynatrace.
   */
  private String deviceId;

  /**
   * Technology type for exported metrics. Used to group metrics under a logical
   * technology name in the Dynatrace UI.
   */
  private String technologyType = "java";

  /**
   * URI to ship metrics to. Should be used for SaaS, self managed instances or to
   * en-route through an internal proxy.
   */
  private String uri;

  public String getApiToken() {
    return this.apiToken;
  }

  public RatpackDynatraceConfig apiToken(String apiToken) {
    this.apiToken = apiToken;
    return this;
  }

  public String getDeviceId() {
    return this.deviceId;
  }

  public RatpackDynatraceConfig deviceId(String deviceId) {
    this.deviceId = deviceId;
    return this;
  }

  public String getTechnologyType() {
    return this.technologyType;
  }

  public RatpackDynatraceConfig technologyType(String technologyType) {
    this.technologyType = technologyType;
    return this;
  }

  public String getUri() {
    return this.uri;
  }

  public RatpackDynatraceConfig uri(String uri) {
    this.uri = uri;
    return this;
  }
}
