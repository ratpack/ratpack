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
import com.netflix.hystrix.HystrixCircuitBreaker;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.util.HystrixRollingNumberEvent;
import ratpack.func.Function;

import java.io.StringWriter;

/**
 * This code is taken from hystrix-metrics-event-stream.
 *
 * @see <a href="https://github.com/Netflix/Hystrix/blob/master/hystrix-contrib/hystrix-metrics-event-stream/src/main/java/com/netflix/hystrix/contrib/metrics/eventstream/HystrixMetricsPoller.java" target="_blank">Hystrix - HystrixMetricsPoller.java</a>
 */
public class HystrixCommandMetricsJsonMapper implements Function<HystrixCommandMetrics, String> {
  private final JsonFactory jsonFactory = new JsonFactory();

  @Override
  public String apply(HystrixCommandMetrics commandMetrics) throws Exception {
    HystrixCommandKey key = commandMetrics.getCommandKey();
    HystrixCircuitBreaker circuitBreaker = HystrixCircuitBreaker.Factory.getInstance(key);

    StringWriter jsonString = new StringWriter();
    JsonGenerator json = jsonFactory.createGenerator(jsonString);

    json.writeStartObject();
    json.writeStringField("type", "HystrixCommand");
    json.writeStringField("name", key.name());
    json.writeStringField("group", commandMetrics.getCommandGroup().name());
    json.writeNumberField("currentTime", System.currentTimeMillis());
    // circuit breaker
    if (circuitBreaker == null) {
    // circuit breaker is disabled and thus never open
      json.writeBooleanField("isCircuitBreakerOpen", false);
    } else {
      json.writeBooleanField("isCircuitBreakerOpen", circuitBreaker.isOpen());
    }
    HystrixCommandMetrics.HealthCounts healthCounts = commandMetrics.getHealthCounts();
    json.writeNumberField("errorPercentage", healthCounts.getErrorPercentage());
    json.writeNumberField("errorCount", healthCounts.getErrorCount());
    json.writeNumberField("requestCount", healthCounts.getTotalRequests());
    // rolling counters
    json.writeNumberField("rollingCountBadRequests", commandMetrics.getRollingCount(HystrixRollingNumberEvent.BAD_REQUEST));
    json.writeNumberField("rollingCountCollapsedRequests", commandMetrics.getRollingCount(HystrixRollingNumberEvent.COLLAPSED));
    json.writeNumberField("rollingCountEmit", commandMetrics.getRollingCount(HystrixRollingNumberEvent.EMIT));
    json.writeNumberField("rollingCountExceptionsThrown", commandMetrics.getRollingCount(HystrixRollingNumberEvent.EXCEPTION_THROWN));
    json.writeNumberField("rollingCountFailure", commandMetrics.getRollingCount(HystrixRollingNumberEvent.FAILURE));
    json.writeNumberField("rollingCountEmit", commandMetrics.getRollingCount(HystrixRollingNumberEvent.FALLBACK_EMIT));
    json.writeNumberField("rollingCountFallbackFailure", commandMetrics.getRollingCount(HystrixRollingNumberEvent.FALLBACK_FAILURE));
    json.writeNumberField("rollingCountFallbackRejection", commandMetrics.getRollingCount(HystrixRollingNumberEvent.FALLBACK_REJECTION));
    json.writeNumberField("rollingCountFallbackSuccess", commandMetrics.getRollingCount(HystrixRollingNumberEvent.FALLBACK_SUCCESS));
    json.writeNumberField("rollingCountResponsesFromCache", commandMetrics.getRollingCount(HystrixRollingNumberEvent.RESPONSE_FROM_CACHE));
    json.writeNumberField("rollingCountSemaphoreRejected", commandMetrics.getRollingCount(HystrixRollingNumberEvent.SEMAPHORE_REJECTED));
    json.writeNumberField("rollingCountShortCircuited", commandMetrics.getRollingCount(HystrixRollingNumberEvent.SHORT_CIRCUITED));
    json.writeNumberField("rollingCountSuccess", commandMetrics.getRollingCount(HystrixRollingNumberEvent.SUCCESS));
    json.writeNumberField("rollingCountThreadPoolRejected", commandMetrics.getRollingCount(HystrixRollingNumberEvent.THREAD_POOL_REJECTED));
    json.writeNumberField("rollingCountTimeout", commandMetrics.getRollingCount(HystrixRollingNumberEvent.TIMEOUT));
    json.writeNumberField("currentConcurrentExecutionCount", commandMetrics.getCurrentConcurrentExecutionCount());
    json.writeNumberField("rollingMaxConcurrentExecutionCount", commandMetrics.getRollingMaxConcurrentExecutions());
    // latency percentiles
    json.writeNumberField("latencyExecute_mean", commandMetrics.getExecutionTimeMean());
    json.writeObjectFieldStart("latencyExecute");
    json.writeNumberField("0", commandMetrics.getExecutionTimePercentile(0));
    json.writeNumberField("25", commandMetrics.getExecutionTimePercentile(25));
    json.writeNumberField("50", commandMetrics.getExecutionTimePercentile(50));
    json.writeNumberField("75", commandMetrics.getExecutionTimePercentile(75));
    json.writeNumberField("90", commandMetrics.getExecutionTimePercentile(90));
    json.writeNumberField("95", commandMetrics.getExecutionTimePercentile(95));
    json.writeNumberField("99", commandMetrics.getExecutionTimePercentile(99));
    json.writeNumberField("99.5", commandMetrics.getExecutionTimePercentile(99.5));
    json.writeNumberField("100", commandMetrics.getExecutionTimePercentile(100));
    json.writeEndObject();
    //
    json.writeNumberField("latencyTotal_mean", commandMetrics.getTotalTimeMean());
    json.writeObjectFieldStart("latencyTotal");
    json.writeNumberField("0", commandMetrics.getTotalTimePercentile(0));
    json.writeNumberField("25", commandMetrics.getTotalTimePercentile(25));
    json.writeNumberField("50", commandMetrics.getTotalTimePercentile(50));
    json.writeNumberField("75", commandMetrics.getTotalTimePercentile(75));
    json.writeNumberField("90", commandMetrics.getTotalTimePercentile(90));
    json.writeNumberField("95", commandMetrics.getTotalTimePercentile(95));
    json.writeNumberField("99", commandMetrics.getTotalTimePercentile(99));
    json.writeNumberField("99.5", commandMetrics.getTotalTimePercentile(99.5));
    json.writeNumberField("100", commandMetrics.getTotalTimePercentile(100));
    json.writeEndObject();
    // property values for reporting what is actually seen by the command rather than what was set somewhere
    HystrixCommandProperties commandProperties = commandMetrics.getProperties();
    json.writeNumberField("propertyValue_circuitBreakerRequestVolumeThreshold", commandProperties.circuitBreakerRequestVolumeThreshold().get());
    json.writeNumberField("propertyValue_circuitBreakerSleepWindowInMilliseconds", commandProperties.circuitBreakerSleepWindowInMilliseconds().get());
    json.writeNumberField("propertyValue_circuitBreakerErrorThresholdPercentage", commandProperties.circuitBreakerErrorThresholdPercentage().get());
    json.writeBooleanField("propertyValue_circuitBreakerForceOpen", commandProperties.circuitBreakerForceOpen().get());
    json.writeBooleanField("propertyValue_circuitBreakerForceClosed", commandProperties.circuitBreakerForceClosed().get());
    json.writeBooleanField("propertyValue_circuitBreakerEnabled", commandProperties.circuitBreakerEnabled().get());
    json.writeStringField("propertyValue_executionIsolationStrategy", commandProperties.executionIsolationStrategy().get().name());
    json.writeNumberField("propertyValue_executionIsolationThreadTimeoutInMilliseconds", commandProperties.executionTimeoutInMilliseconds().get());
    json.writeNumberField("propertyValue_executionTimeoutInMilliseconds", commandProperties.executionTimeoutInMilliseconds().get());
    json.writeBooleanField("propertyValue_executionIsolationThreadInterruptOnTimeout", commandProperties.executionIsolationThreadInterruptOnTimeout().get());
    json.writeStringField("propertyValue_executionIsolationThreadPoolKeyOverride", commandProperties.executionIsolationThreadPoolKeyOverride().get());
    json.writeNumberField("propertyValue_executionIsolationSemaphoreMaxConcurrentRequests", commandProperties.executionIsolationSemaphoreMaxConcurrentRequests().get());
    json.writeNumberField("propertyValue_fallbackIsolationSemaphoreMaxConcurrentRequests", commandProperties.fallbackIsolationSemaphoreMaxConcurrentRequests().get());
    /*
    * The following are commented out as these rarely change and are verbose for streaming for something people don't change.
    * We could perhaps allow a property or request argument to include these.
    */
    // json.put("propertyValue_metricsRollingPercentileEnabled", commandProperties.metricsRollingPercentileEnabled().get());
    // json.put("propertyValue_metricsRollingPercentileBucketSize", commandProperties.metricsRollingPercentileBucketSize().get());
    // json.put("propertyValue_metricsRollingPercentileWindow", commandProperties.metricsRollingPercentileWindowInMilliseconds().get());
    // json.put("propertyValue_metricsRollingPercentileWindowBuckets", commandProperties.metricsRollingPercentileWindowBuckets().get());
    // json.put("propertyValue_metricsRollingStatisticalWindowBuckets", commandProperties.metricsRollingStatisticalWindowBuckets().get());
    json.writeNumberField("propertyValue_metricsRollingStatisticalWindowInMilliseconds", commandProperties.metricsRollingStatisticalWindowInMilliseconds().get());
    json.writeBooleanField("propertyValue_requestCacheEnabled", commandProperties.requestCacheEnabled().get());
    json.writeBooleanField("propertyValue_requestLogEnabled", commandProperties.requestLogEnabled().get());
    json.writeNumberField("reportingHosts", 1); // this will get summed across all instances in a cluster
    json.writeEndObject();
    json.close();
    return jsonString.getBuffer().toString();
  }

}
