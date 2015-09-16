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

package ratpack.dropwizard.metrics;

import com.codahale.metrics.MetricFilter;
import io.netty.buffer.ByteBufAllocator;
import ratpack.dropwizard.metrics.internal.MetricRegistryJsonMapper;
import ratpack.dropwizard.metrics.internal.MetricsBroadcaster;
import ratpack.dropwizard.metrics.internal.RegexMetricFilter;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import static ratpack.websocket.WebSockets.websocketByteBufBroadcast;

/**
 * A Handler that broadcasts metric reports via web sockets.
 * <p>
 * This handler should be bound to an application path, and most likely only for the GET method…
 * <pre class="java-chain-dsl">
 * import ratpack.dropwizard.metrics.MetricsWebsocketBroadcastHandler;
 * import static org.junit.Assert.*;
 *
 * assertTrue(chain instanceof ratpack.handling.Chain);
 * chain.get("admin/metrics", new MetricsWebsocketBroadcastHandler());
 * </pre>
 */
public class MetricsWebsocketBroadcastHandler implements Handler {

  @Override
  public void handle(final Context context) throws Exception {
    final MetricsBroadcaster broadcaster = context.get(MetricsBroadcaster.class);
    final ByteBufAllocator byteBufAllocator = context.get(ByteBufAllocator.class);
    final DropwizardMetricsConfig config = context.get(DropwizardMetricsConfig.class);

    MetricFilter filter = MetricFilter.ALL;
    if (config.getWebSocket().isPresent()) {
      filter = new RegexMetricFilter(config.getWebSocket().get().getIncludeFilter(), config.getWebSocket().get().getExcludeFilter());
    }

    websocketByteBufBroadcast(
      context,
      broadcaster.map(new MetricRegistryJsonMapper(byteBufAllocator, filter))
    );
  }

}
