package ratpack.micrometer.metrics.config;

public class RatpackMeterRegistryConfig<C> {
  private boolean enabled;

  public boolean isEnabled() {
    return enabled;
  }

  @SuppressWarnings("unchecked")
  public C enabled(boolean enabled) {
    this.enabled = enabled;
    return (C) this;
  }
}
