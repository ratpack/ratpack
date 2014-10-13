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

package ratpack.hystrix;

import org.reactivestreams.Publisher;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.hystrix.internal.HystrixCommandMetricsBroadcaster;
import ratpack.hystrix.internal.HystrixCommandMetricsJsonMapper;
import ratpack.hystrix.internal.HystrixThreadPoolMetricsBroadcaster;
import ratpack.hystrix.internal.HystrixThreadPoolMetricsJsonMapper;

import static ratpack.sse.ServerSentEvents.serverSentEvents;
import static ratpack.stream.Streams.*;

/**
 * A Handler that streams Hystrix metrics in text/event-stream format.
 * <p>
 * This handler should be bound to an application path, and most likely only for the GET method…
 * <pre class="java-chain-dsl">
 * import ratpack.hystrix.HystrixMetricsEventStreamHandler;
 *
 * chain instanceof ratpack.handling.Chain;
 * chain.get("admin/hystrix.stream", new HystrixMetricsEventStreamHandler());
 * </pre>
 * <p>
 * This handler can be used in conjunction with Server Sent Event based clients such as the
 * <a href="https://github.com/Netflix/Hystrix/wiki/Dashboard">Hystrix Dashboard</a> and
 * <a href="https://github.com/Netflix/Turbine/wiki">Turbine</a> to consume the metrics being reported by your
 * application in realtime.
 *
 * @see <a href="https://github.com/Netflix/Hystrix/wiki" target="_blank">Hystrix</a>
 * @see ratpack.sse.internal.ServerSentEventsRenderer
 */
public class HystrixMetricsEventStreamHandler implements Handler {

  @Override
  public void handle(Context context) throws Exception {
    final HystrixCommandMetricsBroadcaster commandMetricsBroadcasterbroadcaster = context.get(HystrixCommandMetricsBroadcaster.class);
    final HystrixCommandMetricsJsonMapper commandMetricsMapper = context.get(HystrixCommandMetricsJsonMapper.class);
    final HystrixThreadPoolMetricsBroadcaster threadPoolMetricsBroadcaster = context.get(HystrixThreadPoolMetricsBroadcaster.class);
    final HystrixThreadPoolMetricsJsonMapper threadPoolMetricsMapper = context.get(HystrixThreadPoolMetricsJsonMapper.class);

    Publisher<String> commandStream = map(fanOut(commandMetricsBroadcasterbroadcaster), commandMetricsMapper);
    Publisher<String> threadPoolStream = map(fanOut(threadPoolMetricsBroadcaster), threadPoolMetricsMapper);
    @SuppressWarnings("unchecked") Publisher<String> metricsStream = merge(commandStream, threadPoolStream);

    context.render(serverSentEvents(metricsStream, spec -> spec.data(spec.getItem())));
  }

}
