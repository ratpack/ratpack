package ratpack.micrometer.metrics.internal;

import io.micrometer.core.instrument.MeterRegistry;
import ratpack.handling.Context;
import ratpack.micrometer.metrics.MicrometerMetricsConfig;
import ratpack.micrometer.metrics.RequestTimingHandler;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Provide an instance of a request timing handler.
 */
public class RequestTimingHandlerProvider implements Provider<RequestTimingHandler> {

  private final MeterRegistry meterRegistry;
  private final MicrometerMetricsConfig config;

  @Inject
  public RequestTimingHandlerProvider(MeterRegistry meterRegistry, MicrometerMetricsConfig config) {
    this.meterRegistry = meterRegistry;
    this.config = config;
  }

  @Override
  public RequestTimingHandler get() {
    return config.isRequestTimingMetrics() ? new DefaultRequestTimingHandler(config, meterRegistry) : Context::next;
  }
}
