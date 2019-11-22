package ratpack.micrometer.metrics.config;

public class RatpackKairosConfig extends RatpackStepRegistryConfig<RatpackKairosConfig> {
  /**
   * URI of the KairosDB server.
   */
  private String uri = "http://localhost:8080/api/v1/datapoints";

  /**
   * Login user of the KairosDB server.
   */
  private String userName;

  /**
   * Login password of the KairosDB server.
   */
  private String password;

  public String getUri() {
    return this.uri;
  }

  public RatpackKairosConfig uri(String uri) {
    this.uri = uri;
    return this;
  }

  public String getUserName() {
    return this.userName;
  }

  public RatpackKairosConfig userName(String userName) {
    this.userName = userName;
    return this;
  }

  public String getPassword() {
    return this.password;
  }

  public RatpackKairosConfig password(String password) {
    this.password = password;
    return this;
  }
}
