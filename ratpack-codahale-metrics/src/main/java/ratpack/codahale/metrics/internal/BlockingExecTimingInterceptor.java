/*
 * Copyright 2014 the original author or authors.
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

package ratpack.codahale.metrics.internal;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import ratpack.codahale.metrics.CodaHaleMetricsModule;
import ratpack.exec.ExecInterceptor;
import ratpack.exec.Execution;
import ratpack.func.Block;
import ratpack.http.Request;

import java.util.Map;
import java.util.Optional;

@Singleton
public class BlockingExecTimingInterceptor implements ExecInterceptor {

  private final MetricRegistry metricRegistry;
  private final CodaHaleMetricsModule.Config config;

  @Inject
  public BlockingExecTimingInterceptor(MetricRegistry metricRegistry, CodaHaleMetricsModule.Config config) {
    this.metricRegistry = metricRegistry;
    this.config = config;
  }

  @Override
  public void intercept(Execution execution, ExecType type, Block continuation) throws Exception {
    if (type == ExecType.BLOCKING) {
      Optional<Request> requestOpt = execution.maybeGet(Request.class);
      if (requestOpt.isPresent()) {
        Request request = requestOpt.get();
        String tag = buildBlockingTimerTag(request.getPath(), request.getMethod().getName());
        Timer.Context timer = metricRegistry.timer(tag).time();
        try {
          continuation.execute();
        } finally {
          timer.stop();
        }
        return;
      }
    }

    continuation.execute();
  }

  private String buildBlockingTimerTag(String requestPath, String requestMethod) {
    String tagName = requestPath.equals("") ? "root" : requestPath.replace("/", ".");

    if (config.getRequestMetricGroups() != null) {
      for (Map.Entry<String, String> metricGrouping : config.getRequestMetricGroups().entrySet()) {
        if (requestPath.matches(metricGrouping.getValue())) {
          tagName = metricGrouping.getKey();
          break;
        }
      }
    }

    return tagName + "." + requestMethod.toLowerCase() + "-blocking";
  }

}
