package ratpack.micrometer.metrics.internal;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import ratpack.exec.ExecInterceptor;
import ratpack.exec.Execution;
import ratpack.func.Block;
import ratpack.handling.Context;
import ratpack.micrometer.metrics.BlockingExecTimingInterceptor;
import ratpack.micrometer.metrics.MicrometerMetricsConfig;

import java.util.Optional;

public class DefaultBlockingExecTimingInterceptor implements BlockingExecTimingInterceptor {
  private final MicrometerMetricsConfig config;
  private final MeterRegistry meterRegistry;

  DefaultBlockingExecTimingInterceptor(MicrometerMetricsConfig config, MeterRegistry meterRegistry) {
    this.config = config;
    this.meterRegistry = meterRegistry;
  }

  @Override
  public void intercept(Execution execution, ExecInterceptor.ExecType type, Block executionSegment) throws Exception {
    if (type == ExecInterceptor.ExecType.BLOCKING) {
      Optional<Context> contextOpt = execution.maybeGet(Context.class);
      if (contextOpt.isPresent()) {
        Context context = contextOpt.get();
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
          executionSegment.execute();
          sample.stop(meterRegistry.timer("http.blocking.execution",
            config.getHandlerTags().apply(context, null)));
        } catch(Exception e) {
          sample.stop(meterRegistry.timer("http.blocking.execution",
            config.getHandlerTags().apply(context, e)));
          throw e;
        }
      }
    }

    executionSegment.execute();
  }
}
