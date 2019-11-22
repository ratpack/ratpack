package ratpack.micrometer.metrics.config;

import java.util.HashMap;
import java.util.Map;

public class RatpackHumioConfig extends RatpackStepRegistryConfig<RatpackHumioConfig> {
  /**
   * Humio API token.
   */
  private String apiToken;

  /**
   * Humio tags describing the data source in which metrics will be stored. Humio tags
   * are a distinct concept from Micrometer's tags. Micrometer's tags are used to divide
   * metrics along dimensional boundaries.
   */
  private Map<String, String> tags = new HashMap<>();

  /**
   * URI to ship metrics to. If you need to publish metrics to an internal proxy
   * en-route to Humio, you can define the location of the proxy with this.
   */
  private String uri = "https://cloud.humio.com";

  public String getApiToken() {
    return this.apiToken;
  }

  public RatpackHumioConfig apiToken(String apiToken) {
    this.apiToken = apiToken;
    return this;
  }

  public Map<String, String> getTags() {
    return this.tags;
  }

  public RatpackHumioConfig tags(Map<String, String> tags) {
    this.tags = tags;
    return this;
  }

  public String getUri() {
    return this.uri;
  }

  public RatpackHumioConfig uri(String uri) {
    this.uri = uri;
    return this;
  }
}
