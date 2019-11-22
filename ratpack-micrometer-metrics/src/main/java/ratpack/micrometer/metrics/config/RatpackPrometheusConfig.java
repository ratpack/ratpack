package ratpack.micrometer.metrics.config;

import java.time.Duration;

public class RatpackPrometheusConfig extends RatpackMeterRegistryConfig<RatpackPrometheusConfig> {
  /**
   * Whether to enable publishing descriptions as part of the scrape payload to
   * Prometheus. Turn this off to minimize the amount of data sent on each scrape.
   */
  private boolean descriptions = true;

  /**
   * Step size (i.e. reporting frequency) to use.
   */
  private Duration step = Duration.ofMinutes(1);

  public boolean isDescriptions() {
    return this.descriptions;
  }

  public RatpackPrometheusConfig descriptions(boolean descriptions) {
    this.descriptions = descriptions;
    return this;
  }

  public Duration getStep() {
    return this.step;
  }

  public RatpackPrometheusConfig step(Duration step) {
    this.step = step;
    return this;
  }
}
