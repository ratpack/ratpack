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

package ratpack.dropwizard.metrics

import com.codahale.metrics.Gauge
import io.netty.buffer.PooledByteBufAllocator
import ratpack.dropwizard.metrics.internal.PooledByteBufAllocatorMetricSet
import spock.lang.Specification

class PooledByteBufAllocatorMetricSetSpec extends Specification {

  def "initialize metric set"() {
    given:
    def allocator = new PooledByteBufAllocator(false, 1, 1, 4096,
      1, 1, 1, 1, false)

    when:
    def metricSet = new PooledByteBufAllocatorMetricSet(allocator)

    then:
    // defaults for PooledByteBufAllocator
    with(metricSet.metrics) { it ->
      size() == 9
      (it.numDirectArenas as Gauge).value == 1
      (it.numHeapArenas as Gauge).value == 1
      (it.numThreadLocalCaches as Gauge).value == 0
      (it.smallCacheSize as Gauge).value == 1
      (it.tinyCacheSize as Gauge).value == 1
      (it.normalCacheSize as Gauge).value == 1
      (it.chunkSize as Gauge).value == 8192
      (it.usedDirectMemory as Gauge).value == 0
      (it.usedHeapMemory as Gauge).value == 0
    }
  }

  def "initialize metric set with detailed metrics - arenas"() {
    given:
    def allocator = new PooledByteBufAllocator(false, 1, 1, 4096,
      1, 1, 1, 1, false)

    when:
    def metricSet = new PooledByteBufAllocatorMetricSet(allocator, true)

    then:
    // defaults for PooledByteBufAllocator
    with(metricSet.metrics) { it ->
      size() == 39
      (it.numDirectArenas as Gauge).value == 1
      (it.numHeapArenas as Gauge).value == 1
      (it.numThreadLocalCaches as Gauge).value == 0
      (it.smallCacheSize as Gauge).value == 1
      (it.tinyCacheSize as Gauge).value == 1
      (it.normalCacheSize as Gauge).value == 1
      (it.chunkSize as Gauge).value == 8192
      (it.usedDirectMemory as Gauge).value == 0
      (it.usedHeapMemory as Gauge).value == 0

      (it.'poolArena.0.numDeallocations' as Gauge).value == 0
      (it.'poolArena.0.numActiveNormalAllocations' as Gauge).value == 0
      (it.'poolArena.0.numSmallAllocations' as Gauge).value == 0
      (it.'poolArena.0.numSmallSubpages' as Gauge).value == 3
      (it.'poolArena.0.numTinySubpages' as Gauge).value == 32
      (it.'poolArena.0.numNormalAllocations' as Gauge).value == 0
      (it.'poolArena.0.numActiveTinyAllocations' as Gauge).value == 0
      (it.'poolArena.0.numTinyAllocations' as Gauge).value == 0
      (it.'poolArena.0.numSmallDeallocations' as Gauge).value == 0
      (it.'poolArena.0.numActiveSmallAllocations' as Gauge).value == 0
      (it.'poolArena.0.numTinyDeallocations' as Gauge).value == 0
      (it.'poolArena.0.numHugeDeallocations' as Gauge).value == 0
      (it.'poolArena.0.numActiveBytes' as Gauge).value == 0
      (it.'poolArena.0.numNormalDeallocations' as Gauge).value == 0
      (it.'poolArena.0.numActiveHugeAllocations' as Gauge).value == 0
      (it.'poolArena.0.numAllocations' as Gauge).value == 0
      (it.'poolArena.0.numHugeAllocations' as Gauge).value == 0
      (it.'poolArena.0.activeAllocations' as Gauge).value == 0


      (it.'poolArena.0.poolChunkList.0.minUsage' as Gauge).value == 1
      (it.'poolArena.0.poolChunkList.0.maxUsage' as Gauge).value == 25
      (it.'poolArena.0.poolChunkList.1.minUsage' as Gauge).value == 1
      (it.'poolArena.0.poolChunkList.1.maxUsage' as Gauge).value == 50
      (it.'poolArena.0.poolChunkList.2.minUsage' as Gauge).value == 25
      (it.'poolArena.0.poolChunkList.2.maxUsage' as Gauge).value == 75
      (it.'poolArena.0.poolChunkList.4.minUsage' as Gauge).value == 75
      (it.'poolArena.0.poolChunkList.4.maxUsage' as Gauge).value == 100
      (it.'poolArena.0.poolChunkList.3.minUsage' as Gauge).value == 50
      (it.'poolArena.0.poolChunkList.3.maxUsage' as Gauge).value == 100
      (it.'poolArena.0.poolChunkList.5.minUsage' as Gauge).value == 100
      (it.'poolArena.0.poolChunkList.5.maxUsage' as Gauge).value == 100
    }
  }
}
