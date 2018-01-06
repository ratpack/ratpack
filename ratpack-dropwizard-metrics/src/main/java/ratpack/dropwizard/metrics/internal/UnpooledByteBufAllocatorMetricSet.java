/*
 * Copyright 2018 the original author or authors.
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

package ratpack.dropwizard.metrics.internal;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import io.netty.buffer.ByteBufAllocatorMetric;
import io.netty.buffer.UnpooledByteBufAllocator;

import java.util.HashMap;
import java.util.Map;

/**
 * Metric set exposing {@link UnpooledByteBufAllocator} metrics about memory allocations.
 */
public class UnpooledByteBufAllocatorMetricSet implements MetricSet {

  private final UnpooledByteBufAllocator unpooledByteBufAllocator;
  private final Map<String, Metric> metrics;

  /**
   * Metric set constructor
   *
   * @param unpooledByteBufAllocator allocator which would expose metrics
   */
  public UnpooledByteBufAllocatorMetricSet(UnpooledByteBufAllocator unpooledByteBufAllocator) {
    this.unpooledByteBufAllocator = unpooledByteBufAllocator;
    this.metrics = new HashMap<>();

    initMetrics();
  }

  private void initMetrics() {
    final ByteBufAllocatorMetric metric = unpooledByteBufAllocator.metric();

    metrics.put("usedDirectMemory", (Gauge<Long>) () -> metric.usedDirectMemory());
    metrics.put("usedHeapMemory", (Gauge<Long>) () -> metric.usedHeapMemory());
  }

  @Override
  public Map<String, Metric> getMetrics() {
    return metrics;
  }
}
