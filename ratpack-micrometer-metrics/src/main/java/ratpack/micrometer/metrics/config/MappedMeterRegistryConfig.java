package ratpack.micrometer.metrics.config;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterRegistryConfig;

import javax.annotation.Nonnull;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class MappedMeterRegistryConfig<T> implements MeterRegistryConfig {
  private T config;

  @Override
  public String get(@Nonnull String key) {
    return null;
  }

  /**
   * Create a new {@link MappedMeterRegistryConfig} instance.
   * @param config the Ratpack-style config
   */
  MappedMeterRegistryConfig(T config) {
    this.config = config;
  }

  /**
   * Get the value from the properties or use a fallback from the {@code defaults}.
   * @param getter the getter for the properties
   * @param fallback the fallback method, usually super interface method reference
   * @param <V> the value type
   * @return the property or fallback value
   */
  protected final <V> V get(Function<T, V> getter, Supplier<V> fallback) {
    V value = getter.apply(this.config);
    return (value != null) ? value : fallback.get();
  }

  public MeterRegistry buildRegistry(Clock clock) { return null; }
}
