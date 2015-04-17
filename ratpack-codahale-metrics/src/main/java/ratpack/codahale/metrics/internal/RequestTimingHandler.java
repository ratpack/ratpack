/*
 * Copyright 2013 the original author or authors.
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
import ratpack.codahale.metrics.CodaHaleMetricsModule;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.Request;

import java.util.Map;

/**
 * A handler implementation that collects {@link Timer} metrics for a {@link Request}.
 * <p>
 * Metrics are grouped by default using {@link ratpack.http.Request#getUri()} and {@link ratpack.http.Request#getMethod()}.
 * For example, the following requests...
 *
 * <pre>
 * /
 * /book
 * /author/1/books
 * </pre>
 *
 * will be reported as...
 *
 * <pre>
 * root.get-requests
 * book.get-requests
 * author.1.books.get-requests
 * </pre>
 *
 * However, custom groupings can be defined using {@link CodaHaleMetricsModule.Config#getRequestMetricGroups()}.
 * For example, applying the following config
 *
 * <pre class="groovy-ratpack-dsl">
 * import ratpack.codahale.metrics.CodaHaleMetricsModule
 * import static ratpack.groovy.Groovy.ratpack
 *
 * ratpack {
 *   bindings {
 *     add new CodaHaleMetricsModule(), { it.requestMetricGroups(['book':'.*book.*']) }
 *   }
 * }
 * </pre>
 *
 * will be reported as...
 *
 * <pre>
 * root.get-requests
 * book.get-requests
 * </pre>
 *
 */
public class RequestTimingHandler implements Handler {

  private final CodaHaleMetricsModule.Config config;

  public RequestTimingHandler(CodaHaleMetricsModule.Config config) {
    this.config = config;
  }

  @Override
  public void handle(final Context context) throws Exception {
    final MetricRegistry metricRegistry = context.get(MetricRegistry.class);
    final Request request = context.getRequest();
    BlockingExecTimingInterceptor blockingExecTimingInterceptor = new BlockingExecTimingInterceptor(metricRegistry, request, config);

    context.addInterceptor(blockingExecTimingInterceptor, () -> {
      String tag = buildRequestTimerTag(request.getUri(), request.getMethod().getName());
      final Timer.Context timer = metricRegistry.timer(tag).time();
      context.onClose(thing -> timer.stop());
      context.next();
    });
  }

  private String buildRequestTimerTag(String requestUri, String requestMethod) {
    String tagName = (requestUri.equals("/") ? "root" : requestUri.replaceFirst("/", "").replace("/", "."));

    if (config.getRequestMetricGroups() != null) {
      for (Map.Entry<String, String> metricGrouping : config.getRequestMetricGroups().entrySet()) {
        if (requestUri.matches(metricGrouping.getValue())) {
          tagName = metricGrouping.getKey();
          break;
        }
      }
    }

    return tagName + "." + requestMethod.toLowerCase() + "-requests";
  }

}
