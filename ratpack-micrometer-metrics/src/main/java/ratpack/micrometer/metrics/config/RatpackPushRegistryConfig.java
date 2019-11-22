package ratpack.micrometer.metrics.config;

import java.time.Duration;

@SuppressWarnings("unchecked")
public abstract class RatpackPushRegistryConfig<C> extends RatpackMeterRegistryConfig<C> {
  /**
    * Step size (i.e. reporting frequency) to use.
    */
  private Duration step = Duration.ofMinutes(1);

  /**
   * Whether exporting of metrics to this backend is enabled.
   */
  private boolean enabled = true;

  /**
   * Connection timeout for requests to this backend.
   */
  private Duration connectTimeout = Duration.ofSeconds(1);

  /**
   * Read timeout for requests to this backend.
   */
  private Duration readTimeout = Duration.ofSeconds(10);

  /**
   * Number of threads to use with the metrics publishing scheduler.
   */
  private Integer numThreads = 2;

  /**
   * Number of measurements per request to use for this backend. If more measurements
   * are found, then multiple requests will be made.
   */
  private Integer batchSize = 10000;

  public Duration getStep() {
    return this.step;
  }

  public C step(Duration step) {
    this.step = step;
    return (C) this;
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  public C enabled(boolean enabled) {
    this.enabled = enabled;
    return (C) this;
  }

  public Duration getConnectTimeout() {
    return this.connectTimeout;
  }

  public C connectTimeout(Duration connectTimeout) {
    this.connectTimeout = connectTimeout;
    return (C) this;
  }

  public Duration getReadTimeout() {
    return this.readTimeout;
  }

  public C readTimeout(Duration readTimeout) {
    this.readTimeout = readTimeout;
    return (C) this;
  }

  public int getNumThreads() {
    return this.numThreads;
  }

  public C numThreads(int numThreads) {
    this.numThreads = numThreads;
    return (C) this;
  }

  public int getBatchSize() {
    return this.batchSize;
  }

  public C batchSize(int batchSize) {
    this.batchSize = batchSize;
    return (C) this;
  }

  public String get(String key) {
    return null;
  }

  public String prefix() {
    return null;
  }
}
