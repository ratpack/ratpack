package ratpack.micrometer.metrics.config;

public class RatpackNewRelicConfig extends RatpackStepRegistryConfig<RatpackNewRelicConfig> {
  /**
   * Whether to send the meter name as the event type instead of using the 'event-type'
   * configuration property value. Can be set to 'true' if New Relic guidelines are not
   * being followed or event types consistent with previous Spring Boot releases are
   * required.
   */
  private boolean meterNameEventTypeEnabled;

  /**
   * The event type that should be published. This property will be ignored if
   * 'meter-name-event-type-enabled' is set to 'true'.
   */
  private String eventType = "SpringBootSample";

  /**
   * New Relic API key.
   */
  private String apiKey;

  /**
   * New Relic account ID.
   */
  private String accountId;

  /**
   * URI to ship metrics to.
   */
  private String uri = "https://insights-collector.newrelic.com";

  public boolean isMeterNameEventTypeEnabled() {
    return this.meterNameEventTypeEnabled;
  }

  public RatpackNewRelicConfig meterNameEventTypeEnabled(boolean meterNameEventTypeEnabled) {
    this.meterNameEventTypeEnabled = meterNameEventTypeEnabled;
    return this;
  }

  public String getEventType() {
    return this.eventType;
  }

  public RatpackNewRelicConfig eventType(String eventType) {
    this.eventType = eventType;
    return this;
  }

  public String getApiKey() {
    return this.apiKey;
  }

  public RatpackNewRelicConfig apiKey(String apiKey) {
    this.apiKey = apiKey;
    return this;
  }

  public String getAccountId() {
    return this.accountId;
  }

  public RatpackNewRelicConfig accountId(String accountId) {
    this.accountId = accountId;
    return this;
  }

  public String getUri() {
    return this.uri;
  }

  public RatpackNewRelicConfig uri(String uri) {
    this.uri = uri;
    return this;
  }
}
