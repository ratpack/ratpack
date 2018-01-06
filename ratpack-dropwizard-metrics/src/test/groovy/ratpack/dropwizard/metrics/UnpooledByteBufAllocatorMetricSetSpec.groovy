package ratpack.dropwizard.metrics

import com.codahale.metrics.Gauge
import io.netty.buffer.UnpooledByteBufAllocator
import spock.lang.Specification

class UnpooledByteBufAllocatorMetricSetSpec extends Specification {

  def "initialize metric set"() {
    given:
    def allocator = new UnpooledByteBufAllocator(false);

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
