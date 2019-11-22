package ratpack.micrometer.metrics.config;

import java.net.URI;

public class RatpackWavefrontConfig extends RatpackStepRegistryConfig<RatpackWavefrontConfig> {
  /**
   * URI to ship metrics to.
   */
  private URI uri = URI.create("https://longboard.wavefront.com");

  /**
   * Unique identifier for the app instance that is the source of metrics being
   * published to Wavefront. Defaults to the local host name.
   */
  private String source;

  /**
   * API token used when publishing metrics directly to the Wavefront API host.
   */
  private String apiToken;

  /**
   * Global prefix to separate metrics originating from this app's white box
   * instrumentation from those originating from other Wavefront integrations when
   * viewed in the Wavefront UI.
   */
  private String globalPrefix;

  public String getUri() {
    return this.uri.toString();
  }

  public RatpackWavefrontConfig uri(URI uri) {
    this.uri = uri;
    return this;
  }

  public String getSource() {
    return this.source;
  }

  public RatpackWavefrontConfig source(String source) {
    this.source = source;
    return this;
  }

  public String getApiToken() {
    return this.apiToken;
  }

  public RatpackWavefrontConfig apiToken(String apiToken) {
    this.apiToken = apiToken;
    return this;
  }

  public String getGlobalPrefix() {
    return this.globalPrefix;
  }

  public RatpackWavefrontConfig globalPrefix(String globalPrefix) {
    this.globalPrefix = globalPrefix;
    return this;
  }
}
