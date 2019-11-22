package ratpack.micrometer.metrics.config;

public class RatpackSignalFxConfig extends RatpackStepRegistryConfig<RatpackSignalFxConfig> {
  /**
   * SignalFX access token.
   */
  private String accessToken;

  /**
   * URI to ship metrics to.
   */
  private String uri = "https://ingest.signalfx.com";

  /**
   * Uniquely identifies the app instance that is publishing metrics to SignalFx.
   * Defaults to the local host name.
   */
  private String source;

  public String getAccessToken() {
    return this.accessToken;
  }

  public RatpackSignalFxConfig accessToken(String accessToken) {
    this.accessToken = accessToken;
    return this;
  }

  public String getUri() {
    return this.uri;
  }

  public RatpackSignalFxConfig uri(String uri) {
    this.uri = uri;
    return this;
  }

  public String getSource() {
    return this.source;
  }

  public RatpackSignalFxConfig source(String source) {
    this.source = source;
    return this;
  }
}
