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

package ratpack.hystrix.internal;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolMetrics;
import com.netflix.hystrix.util.HystrixRollingNumberEvent;
import ratpack.func.Function;

import java.io.StringWriter;

/**
 * This code is taken from hystrix-metrics-event-stream.
 *
 * @see <a href="https://github.com/Netflix/Hystrix/blob/master/hystrix-contrib/hystrix-metrics-event-stream/src/main/java/com/netflix/hystrix/contrib/metrics/eventstream/HystrixMetricsPoller.java" target="_blank">Hystrix - HystrixMetricsPoller.java</a>
 */
public class HystrixThreadPoolMetricsJsonMapper implements Function<HystrixThreadPoolMetrics, String> {
  private final JsonFactory jsonFactory = new JsonFactory();

  @Override
  public String apply(HystrixThreadPoolMetrics threadPoolMetrics) throws Exception {
    HystrixThreadPoolKey key = threadPoolMetrics.getThreadPoolKey();
    StringWriter jsonString = new StringWriter();
    JsonGenerator json = jsonFactory.createGenerator(jsonString);
    json.writeStartObject();
    json.writeStringField("type", "HystrixThreadPool");
    json.writeStringField("name", key.name());
    json.writeNumberField("currentTime", System.currentTimeMillis());
    json.writeNumberField("currentActiveCount", threadPoolMetrics.getCurrentActiveCount().intValue());
    json.writeNumberField("currentCompletedTaskCount", threadPoolMetrics.getCurrentCompletedTaskCount().longValue());
    json.writeNumberField("currentCorePoolSize", threadPoolMetrics.getCurrentCorePoolSize().intValue());
    json.writeNumberField("currentLargestPoolSize", threadPoolMetrics.getCurrentLargestPoolSize().intValue());
    json.writeNumberField("currentMaximumPoolSize", threadPoolMetrics.getCurrentMaximumPoolSize().intValue());
    json.writeNumberField("currentPoolSize", threadPoolMetrics.getCurrentPoolSize().intValue());
    json.writeNumberField("currentQueueSize", threadPoolMetrics.getCurrentQueueSize().intValue());
    json.writeNumberField("currentTaskCount", threadPoolMetrics.getCurrentTaskCount().longValue());
    json.writeNumberField("rollingCountThreadsExecuted", threadPoolMetrics.getRollingCount(HystrixRollingNumberEvent.THREAD_EXECUTION));
    json.writeNumberField("rollingMaxActiveThreads", threadPoolMetrics.getRollingMaxActiveThreads());
    json.writeNumberField("rollingCountCommandRejections", threadPoolMetrics.getRollingCount(HystrixRollingNumberEvent.THREAD_POOL_REJECTED));
    json.writeNumberField("propertyValue_queueSizeRejectionThreshold", threadPoolMetrics.getProperties().queueSizeRejectionThreshold().get());
    json.writeNumberField("propertyValue_metricsRollingStatisticalWindowInMilliseconds", threadPoolMetrics.getProperties().metricsRollingStatisticalWindowInMilliseconds().get());
    json.writeNumberField("reportingHosts", 1); // this will get summed across all instances in a cluster
    json.writeEndObject();
    json.close();
    return jsonString.getBuffer().toString();
  }

}
