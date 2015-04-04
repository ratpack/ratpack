/*
 * Copyright 2015 the original author or authors.
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
import com.netflix.hystrix.HystrixCollapserKey;
import com.netflix.hystrix.HystrixCollapserMetrics;
import com.netflix.hystrix.util.HystrixRollingNumberEvent;
import ratpack.func.Function;

import java.io.StringWriter;

/**
 * This code is taken from hystrix-metrics-event-stream.
 *
 * @see <a href="https://github.com/Netflix/Hystrix/blob/master/hystrix-contrib/hystrix-metrics-event-stream/src/main/java/com/netflix/hystrix/contrib/metrics/eventstream/HystrixMetricsPoller.java" target="_blank">Hystrix - HystrixMetricsPoller.java</a>
 */
public class HystrixCollapserMetricsJsonMapper implements Function<HystrixCollapserMetrics, String> {
  private final JsonFactory jsonFactory = new JsonFactory();

  @Override
  public String apply(HystrixCollapserMetrics collapserMetrics) throws Exception {
    HystrixCollapserKey key = collapserMetrics.getCollapserKey();
    StringWriter jsonString = new StringWriter();
    JsonGenerator json = jsonFactory.createGenerator(jsonString);
    json.writeStartObject();
    json.writeStringField("type", "HystrixCollapser");
    json.writeStringField("name", key.name());
    json.writeNumberField("currentTime", System.currentTimeMillis());
    json.writeNumberField("rollingCountRequestsBatched", collapserMetrics.getRollingCount(HystrixRollingNumberEvent.COLLAPSER_REQUEST_BATCHED));
    json.writeNumberField("rollingCountBatches", collapserMetrics.getRollingCount(HystrixRollingNumberEvent.COLLAPSER_BATCH));
    json.writeNumberField("rollingCountResponsesFromCache", collapserMetrics.getRollingCount(HystrixRollingNumberEvent.RESPONSE_FROM_CACHE));
    // batch size percentiles
    json.writeNumberField("batchSize_mean", collapserMetrics.getBatchSizeMean());
    json.writeObjectFieldStart("batchSize");
    json.writeNumberField("25", collapserMetrics.getBatchSizePercentile(25));
    json.writeNumberField("50", collapserMetrics.getBatchSizePercentile(50));
    json.writeNumberField("75", collapserMetrics.getBatchSizePercentile(75));
    json.writeNumberField("90", collapserMetrics.getBatchSizePercentile(90));
    json.writeNumberField("95", collapserMetrics.getBatchSizePercentile(95));
    json.writeNumberField("99", collapserMetrics.getBatchSizePercentile(99));
    json.writeNumberField("99.5", collapserMetrics.getBatchSizePercentile(99.5));
    json.writeNumberField("100", collapserMetrics.getBatchSizePercentile(100));
    json.writeEndObject();
    // shard size percentiles (commented-out for now)
    //json.writeNumberField("shardSize_mean", collapserMetrics.getShardSizeMean());
    //json.writeObjectFieldStart("shardSize");
    //json.writeNumberField("25", collapserMetrics.getShardSizePercentile(25));
    //json.writeNumberField("50", collapserMetrics.getShardSizePercentile(50));
    //json.writeNumberField("75", collapserMetrics.getShardSizePercentile(75));
    //json.writeNumberField("90", collapserMetrics.getShardSizePercentile(90));
    //json.writeNumberField("95", collapserMetrics.getShardSizePercentile(95));
    //json.writeNumberField("99", collapserMetrics.getShardSizePercentile(99));
    //json.writeNumberField("99.5", collapserMetrics.getShardSizePercentile(99.5));
    //json.writeNumberField("100", collapserMetrics.getShardSizePercentile(100));
    //json.writeEndObject();
    //json.writeNumberField("propertyValue_metricsRollingStatisticalWindowInMilliseconds", collapserMetrics.getProperties().metricsRollingStatisticalWindowInMilliseconds().get());
    json.writeBooleanField("propertyValue_requestCacheEnabled", collapserMetrics.getProperties().requestCacheEnabled().get());
    json.writeNumberField("propertyValue_maxRequestsInBatch", collapserMetrics.getProperties().maxRequestsInBatch().get());
    json.writeNumberField("propertyValue_timerDelayInMilliseconds", collapserMetrics.getProperties().timerDelayInMilliseconds().get());
    json.writeNumberField("reportingHosts", 1); // this will get summed across all instances in a cluster
    json.writeEndObject();
    json.close();
    return jsonString.getBuffer().toString();
  }

}
