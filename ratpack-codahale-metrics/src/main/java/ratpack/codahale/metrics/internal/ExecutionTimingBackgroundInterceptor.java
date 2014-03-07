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
import ratpack.handling.BackgroundInterceptor;
import ratpack.handling.Context;
import ratpack.http.Request;

/**
 * A {@link ratpack.handling.BackgroundInterceptor} implementation that collects {@link Timer} metrics
 * for {@link ratpack.handling.Background} executions.
 * <p>
 * Metrics are grouped by {@link ratpack.http.Request#getUri()} and {@link ratpack.http.Request#getMethod()}.
 * For example, the following requests with background tasks...
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
 * [root]~GET~Background
 * [book]~GET~Background
 * [author][1][books]~GET~Background
 * </pre>
 */
public class ExecutionTimingBackgroundInterceptor implements BackgroundInterceptor {

  private Timer.Context timer;

  @Override
  public void toBackground(Context context, Runnable continuation) {
    MetricRegistry metricRegistry = context.get(MetricRegistry.class);
    Request request = context.getRequest();

    String tag = buildBackgroundTimerTag(request.getUri(), request.getMethod().getName());
    timer = metricRegistry.timer(tag).time();
    continuation.run();
  }

  @Override
  public void toForeground(Context context, Runnable continuation) {
    timer.stop();
    continuation.run();
  }

  private String buildBackgroundTimerTag(String requestUri, String requestMethod) {
    return (requestUri.equals("/") ? "[root" : requestUri.replaceFirst("/", "[").replace("/", "][")) + "]~" + requestMethod + "~Background";
  }

}
