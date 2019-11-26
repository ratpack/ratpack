package ratpack.micrometer.metrics.internal;

import io.micrometer.core.instrument.MeterRegistry;
import ratpack.handling.Context;
import ratpack.micrometer.metrics.MicrometerMetricsConfig;
import ratpack.micrometer.metrics.RequestTimingHandler;

import java.util.concurrent.TimeUnit;

public class DefaultRequestTimingHandler implements RequestTimingHandler {
  private final MicrometerMetricsConfig config;
  private final MeterRegistry meterRegistry;

  DefaultRequestTimingHandler(MicrometerMetricsConfig config, MeterRegistry meterRegistry) {
    this.config = config;
    this.meterRegistry = meterRegistry;
  }

  @Override
  public void handle(final Context context) {
    context.onClose(outcome -> {
      meterRegistry.timer("http.server.requests", config.getHandlerTags().apply(context, null))
        .record(outcome.getDuration().toNanos(), TimeUnit.NANOSECONDS);
    });
    context.next();
  }
}
