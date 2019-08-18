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
import io.netty.buffer.UnpooledByteBufAllocator
import ratpack.dropwizard.metrics.internal.UnpooledByteBufAllocatorMetricSet
import spock.lang.Specification

class UnpooledByteBufAllocatorMetricSetSpec extends Specification {

  def "initialize metric set"() {
    given:
    def allocator = new UnpooledByteBufAllocator(false)

    when:
    def metricSet = new UnpooledByteBufAllocatorMetricSet(allocator)

    then:
    with(metricSet.metrics) { it ->
      size() == 2
      (it.usedDirectMemory as Gauge).value == 0
      (it.usedHeapMemory as Gauge).value == 0
    }
  }
}
