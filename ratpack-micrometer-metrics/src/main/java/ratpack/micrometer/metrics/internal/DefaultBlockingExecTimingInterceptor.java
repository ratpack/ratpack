/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
