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

package ratpack.dropwizard.metrics.internal;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import ratpack.dropwizard.metrics.DropwizardMetricsConfig;
import ratpack.exec.ExecController;
import ratpack.http.client.HttpClient;
import ratpack.http.client.internal.DefaultHttpClient;
import ratpack.http.client.internal.HttpClientStats;
import ratpack.service.Service;
import ratpack.service.StartEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
public class HttpClientMetrics implements Service, Runnable {

  private static final String METRIC_PREFIX = "httpclient.";
  private static final String TOTAL_ACTIVE_CONNECTIONS = getMetricName("total.active.connections");
  private static final String TOTAL_IDLE_CONNECTIONS = getMetricName("total.idle.connections");
  private static final String TOTAL_CONNECTIONS = getMetricName("total.connections");

  private final HttpClient httpClient;
  private final MetricRegistry metricRegistry;
  private final DropwizardMetricsConfig config;
  private final ConcurrentMap<String, HttpMetricGauge> gauges;

  @Inject
  public HttpClientMetrics(
    HttpClient httpClient,
    MetricRegistry metricRegistry,
    DropwizardMetricsConfig config
  ) {
    this.httpClient = httpClient;
    this.metricRegistry = metricRegistry;
    this.config = config;
    this.gauges = new ConcurrentHashMap<>();
  }

  @Override
  public void onStart(StartEvent event) throws Exception {
    config.getHttpClient().ifPresent(httpClientConfig -> {
      boolean enabled = httpClientConfig.isEnabled();
      int pollingFrequency = httpClientConfig.getPollingFrequencyInSeconds();

      if (enabled && httpClient instanceof DefaultHttpClient) {
        ExecController execController = event.getRegistry().get(ExecController.class);
        execController.getExecutor().scheduleAtFixedRate(this, 0, pollingFrequency, TimeUnit.SECONDS);
      }
    });
  }

  @Override
  public void run() {
    HttpClientStats httpClientStats = ((DefaultHttpClient) httpClient).getHttpClientStats();
    gauge(TOTAL_ACTIVE_CONNECTIONS).setValue(httpClientStats.getTotalActiveConnectionCount());
    gauge(TOTAL_IDLE_CONNECTIONS).setValue(httpClientStats.getTotalIdleConnectionCount());
    gauge(TOTAL_CONNECTIONS).setValue(httpClientStats.getTotalConnectionCount());
    httpClientStats.getStatsPerHost().forEach((host, stats) -> {
        gauge(getHostMetricName(host, "total.active.connections"))
          .setValue(stats.getActiveConnectionCount());
        gauge(getHostMetricName(host, "total.idle.connections"))
          .setValue(stats.getIdleConnectionCount());
      gauge(getHostMetricName(host, "total.connections"))
          .setValue(stats.getTotalConnectionCount());
      });
  }

  private HttpMetricGauge gauge(String name) {
    if (gauges.containsKey(name)) {
      return gauges.get(name);
    }
    try {
      HttpMetricGauge gauge = metricRegistry.register(name, new HttpMetricGauge());
      gauges.put(name, gauge);
      return gauge;
    } catch (IllegalArgumentException e) {
      // metricRegistry.register throws IllegalArgumentException when metric already exists.
      final HttpMetricGauge gauge = gauges.get(name);
      if (gauge != null && metricRegistry.getNames().contains(name)) {
        return gauge;
      }
    }

    throw new IllegalArgumentException(name + " is already used for a different type of metric");
  }

  private static String getMetricName(String name) {
    return METRIC_PREFIX + name;
  }

  private static String getHostMetricName(String host, String name) {
    return getMetricName(host + "." + name);
  }

  private static class HttpMetricGauge implements Gauge<Long> {

    private AtomicLong value = new AtomicLong(0);

    @Override
    public Long getValue() {
      return value.longValue();
    }

    public void setValue(Long val) {
      value.set(val);
    }
  }
}
