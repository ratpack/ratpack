package ratpack.dropwizard.metrics;

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
