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

package ratpack.codahale.internal;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.handling.RequestOutcome;
import ratpack.http.Request;
import ratpack.util.Action;

public class MetricHandler implements Handler {

  private final Handler rest;

  public MetricHandler(Handler rest) {
    this.rest = rest;
  }

  @Override
  public void handle(Context context) throws Exception {
    Request request = context.getRequest();
    String tag = buildRequestTimerTag(request.getUri(), request.getMethod().getName());

    MetricRegistry metricRegistry = context.get(MetricRegistry.class);
    final Timer.Context timer = metricRegistry.timer(tag).time();

    context.onClose(new Action<RequestOutcome>() {
      public void execute(RequestOutcome thing) throws Exception {
        timer.stop();
      }
    });

    context.insert(rest);
  }

  private String buildRequestTimerTag(String requestUri, String requestMethod) {
    return (requestUri.equals("/") ? "[root" : requestUri.replaceFirst("/", "[").replace("/", "][")) + "]~" + requestMethod + "~Request";
  }

}
