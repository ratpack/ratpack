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
import io.netty.buffer.PoolArenaMetric;
import io.netty.buffer.PoolChunkListMetric;
import io.netty.buffer.PoolSubpageMetric;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocatorMetric;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static java.lang.String.format;

/**
 * Metric set exposing {@link PooledByteBufAllocator} metrics about memory allocations. Metric set
 * can be initialized to provide basic or detailed metrics. Detailed metrics contain information about
 * specific arenas (chunk of memory allocated using malloc).
 */
public class PooledByteBufAllocatorMetricSet implements MetricSet {

  private final PooledByteBufAllocator pooledByteBufAllocator;
  private final boolean includeArenas;
  private final Map<String, Metric> metrics;

  /**
   * Metric set constructor
   *
   * @param pooledByteBufAllocator allocator which would expose metrics
   */
  public PooledByteBufAllocatorMetricSet(PooledByteBufAllocator pooledByteBufAllocator) {
    this(pooledByteBufAllocator, false);
  }

  /**
   * Metric set constructor
   *
   * @param pooledByteBufAllocator allocator which would expose metrics
   * @param includeArenas          boolean whether detailed metrics should be collected(memory arenas)
   */
  public PooledByteBufAllocatorMetricSet(PooledByteBufAllocator pooledByteBufAllocator, boolean includeArenas) {
    this.pooledByteBufAllocator = pooledByteBufAllocator;
    this.includeArenas = includeArenas;
    this.metrics = new HashMap<>();

    initMetrics();
  }

  private void initMetrics() {
    final PooledByteBufAllocatorMetric metric = pooledByteBufAllocator.metric();

    metrics.put("numDirectArenas", (Gauge<Integer>) () -> metric.numDirectArenas());
    metrics.put("numHeapArenas", (Gauge<Integer>) () -> metric.numHeapArenas());
    metrics.put("numThreadLocalCaches", (Gauge<Integer>) () -> metric.numThreadLocalCaches());
    metrics.put("smallCacheSize", (Gauge<Integer>) () -> metric.smallCacheSize());
    metrics.put("tinyCacheSize", (Gauge<Integer>) () -> metric.tinyCacheSize());
    metrics.put("normalCacheSize", (Gauge<Integer>) () -> metric.normalCacheSize());
    metrics.put("chunkSize", (Gauge<Integer>) () -> metric.chunkSize());

    metrics.put("usedDirectMemory", (Gauge<Long>) () -> metric.usedDirectMemory());
    metrics.put("usedHeapMemory", (Gauge<Long>) () -> metric.usedHeapMemory());

    if (includeArenas) {
      final Iterator<PoolArenaMetric> directArenasIterator = metric.directArenas().iterator();
      initPoolArenaMetrics(directArenasIterator);


      final Iterator<PoolArenaMetric> heapArenasIterator = metric.heapArenas().iterator();
      initPoolArenaMetrics(heapArenasIterator);
    }
  }

  private void initPoolArenaMetrics(final Iterator<PoolArenaMetric> poolArenasIterator) {
    int counter = 0;
    while (poolArenasIterator.hasNext()) {
      final PoolArenaMetric poolArenaMetric = poolArenasIterator.next();
      final String prefix = format("poolArena.%s", counter);

      metrics.put(format("%s.activeAllocations", prefix), (Gauge<Long>) () -> poolArenaMetric.numActiveAllocations());
      metrics.put(format("%s.numActiveBytes", prefix), (Gauge<Long>) () -> poolArenaMetric.numActiveBytes());
      metrics.put(format("%s.numActiveHugeAllocations", prefix), (Gauge<Long>) () -> poolArenaMetric.numActiveHugeAllocations());
      metrics.put(format("%s.numActiveNormalAllocations", prefix), (Gauge<Long>) () -> poolArenaMetric.numActiveNormalAllocations());
      metrics.put(format("%s.numActiveSmallAllocations", prefix), (Gauge<Long>) () -> poolArenaMetric.numActiveSmallAllocations());
      metrics.put(format("%s.numActiveTinyAllocations", prefix), (Gauge<Long>) () -> poolArenaMetric.numActiveTinyAllocations());
      metrics.put(format("%s.numAllocations", prefix), (Gauge<Long>) () -> poolArenaMetric.numAllocations());
      metrics.put(format("%s.numDeallocations", prefix), (Gauge<Long>) () -> poolArenaMetric.numDeallocations());
      metrics.put(format("%s.numHugeAllocations", prefix), (Gauge<Long>) () -> poolArenaMetric.numHugeAllocations());
      metrics.put(format("%s.numHugeDeallocations", prefix), (Gauge<Long>) () -> poolArenaMetric.numHugeDeallocations());
      metrics.put(format("%s.numNormalAllocations", prefix), (Gauge<Long>) () -> poolArenaMetric.numNormalAllocations());
      metrics.put(format("%s.numNormalDeallocations", prefix), (Gauge<Long>) () -> poolArenaMetric.numNormalDeallocations());
      metrics.put(format("%s.numSmallAllocations", prefix), (Gauge<Long>) () -> poolArenaMetric.numSmallAllocations());
      metrics.put(format("%s.numSmallDeallocations", prefix), (Gauge<Long>) () -> poolArenaMetric.numSmallDeallocations());
      metrics.put(format("%s.numTinyAllocations", prefix), (Gauge<Long>) () -> poolArenaMetric.numTinyAllocations());
      metrics.put(format("%s.numTinyDeallocations", prefix), (Gauge<Long>) () -> poolArenaMetric.numTinyDeallocations());
      metrics.put(format("%s.numSmallSubpages", prefix), (Gauge<Integer>) () -> poolArenaMetric.numSmallSubpages());
      metrics.put(format("%s.numTinySubpages", prefix), (Gauge<Integer>) () -> poolArenaMetric.numTinySubpages());

      final Iterator<PoolChunkListMetric> chunksIterator = poolArenaMetric.chunkLists().iterator();
      initPoolChunkListMetrics(prefix, chunksIterator);

      final Iterator<PoolSubpageMetric> smallSubpagesIterator = poolArenaMetric.smallSubpages().iterator();
      initSubpageMetrics(prefix, "smallSubpage", smallSubpagesIterator);

      final Iterator<PoolSubpageMetric> tinySubpagesIterator = poolArenaMetric.tinySubpages().iterator();
      initSubpageMetrics(prefix, "tinySubpage", tinySubpagesIterator);

      counter++;
    }
  }

  private void initPoolChunkListMetrics(final String prefix, final Iterator<PoolChunkListMetric> chunksIterator) {
    int counter = 0;
    while (chunksIterator.hasNext()) {
      final PoolChunkListMetric poolChunkMetrics = chunksIterator.next();

      final String poolChunkPrefix = format("poolChunkList.%s", counter);

      metrics.put(format("%s.%s.maxUsage", prefix, poolChunkPrefix), (Gauge<Integer>) () -> poolChunkMetrics.maxUsage());
      metrics.put(format("%s.%s.minUsage", prefix, poolChunkPrefix), (Gauge<Integer>) () -> poolChunkMetrics.minUsage());

      counter++;
    }
  }

  private void initSubpageMetrics(final String prefix, final String subpageName, final Iterator<PoolSubpageMetric> subpagesIterator) {
    int counter = 0;
    while (subpagesIterator.hasNext()) {
      final PoolSubpageMetric poolSubpageMetric = subpagesIterator.next();

      final String subpagePrefix = format("%s.%s", subpageName, counter);

      metrics.put(format("%s.%s.elementSize", prefix, subpagePrefix), (Gauge<Integer>) () -> poolSubpageMetric.elementSize());
      metrics.put(format("%s.%s.maxNumElements", prefix, subpagePrefix), (Gauge<Integer>) () -> poolSubpageMetric.maxNumElements());
      metrics.put(format("%s.%s.numAvailable", prefix, subpagePrefix), (Gauge<Integer>) () -> poolSubpageMetric.numAvailable());
      metrics.put(format("%s.%s.pageSize", prefix, subpagePrefix), (Gauge<Integer>) () -> poolSubpageMetric.pageSize());

      counter++;
    }
  }

  @Override
  public Map<String, Metric> getMetrics() {
    return metrics;
  }
}
