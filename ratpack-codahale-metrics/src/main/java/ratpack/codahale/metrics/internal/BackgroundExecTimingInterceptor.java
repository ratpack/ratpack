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
import ratpack.exec.ExecInterceptor;
import ratpack.http.Request;

public class BackgroundExecTimingInterceptor implements ExecInterceptor {

  private final MetricRegistry metricRegistry;
  private final Request request;

  public BackgroundExecTimingInterceptor(MetricRegistry metricRegistry, Request request) {
    this.metricRegistry = metricRegistry;
    this.request = request;
  }

  @Override
  public void intercept(ExecType type, Runnable continuation) {
    if (type == ExecType.BLOCKING) {
      String tag = buildBackgroundTimerTag(request.getUri(), request.getMethod().getName());
      Timer.Context timer = metricRegistry.timer(tag).time();
      continuation.run();
      timer.stop();
    } else {
      continuation.run();
    }
  }

  private String buildBackgroundTimerTag(String requestUri, String requestMethod) {
    return (requestUri.equals("/") ? "[root" : requestUri.replaceFirst("/", "[").replace("/", "][")) + "]~" + requestMethod + "~Background";
  }

}
