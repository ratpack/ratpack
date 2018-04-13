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
package ratpack.dropwizard.metrics;

import ratpack.dropwizard.metrics.internal.DefaultRequestTimingHandler;
import ratpack.func.Action;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static ratpack.util.Exceptions.uncheck;

/**
 * The configuration object for {@link DropwizardMetricsModule}.
 * <p>
 * Request timing metrics and blocking execution timing metrics are enabled by default.
 */
public class DropwizardMetricsConfig {
  public static final Duration DEFAULT_INTERVAL = Duration.ofSeconds(30);

  private boolean jvmMetrics;
  private boolean prometheusCollection;
  private boolean requestTimingMetrics = true;
  private boolean blockingTimingMetrics = true;
  private Map<String, String> requestMetricGroups;
  private Optional<JmxConfig> jmx = Optional.empty();
  private Optional<ConsoleConfig> console = Optional.empty();
  private Optional<WebsocketConfig> webSocket = Optional.empty();
  private Optional<CsvConfig> csv = Optional.empty();
  private Optional<Slf4jConfig> slf4j = Optional.empty();
  private Optional<GraphiteConfig> graphite = Optional.empty();
  private Optional<ByteBufAllocatorConfig> byteBufAllocator = Optional.empty();
  private Optional<HttpClientConfig> httpClient = Optional.empty();

  /**
   * The state of jvm metrics collection.
   *
   * @return True if jvm metrics collection is enabled. False otherwise
   */
  public boolean isJvmMetrics() {
    return jvmMetrics;
  }

  /**
   * The state of JVM metrics reporting.
   * @param jvmMetrics True if JVM metrics are to be reported. False otherwise
   * @return this
   */
  public DropwizardMetricsConfig jvmMetrics(boolean jvmMetrics) {
    this.jvmMetrics = jvmMetrics;
    return this;
  }

  /**
   * The state of the Prometheus metrics collector.
   *
   * @return True if Prometheus metrics collection is enabled. False otherwise
   * @since 1.6
   */
  public boolean isPrometheusCollection() {
    return prometheusCollection;
  }

  /**
   * The state of Prometheus metrics collection.
   * This method only enables binding the metric registry to the Prometheus collector.
   * The Prometheus formatted metrics can be exposed by adding the {@link MetricsPrometheusHandler} to the handler chain.
   *
   * @param prometheusCollection True if metrics should be collected into the Prometheus collector. False otherwise
   * @return this
   * @see MetricsPrometheusHandler
   * @since 1.6
   */
  public DropwizardMetricsConfig prometheusCollection(boolean prometheusCollection) {
    this.prometheusCollection = prometheusCollection;
    return this;
  }

  /**
   * Get the settings for the byte buf allocator metric set.
   *
   * @return the metric set settings
   * @since 1.6
   */
  public Optional<ByteBufAllocatorConfig> getByteBufAllocator() {
    return byteBufAllocator;
  }

  /**
   * @return this
   * @see #byteBufAllocator(ratpack.func.Action)
   * @since 1.6
   */
  public DropwizardMetricsConfig byteBufAllocator() {
    return byteBufAllocator(Action.noop());
  }

  /**
   * Configure the byte buf allocator metric set.
   *
   * @param configure the configuration for the byte buf allocator metric set
   * @return this
   * @since 1.6
   */
  public DropwizardMetricsConfig byteBufAllocator(Action<? super ByteBufAllocatorConfig> configure) {
    try {
      configure.execute(byteBufAllocator.orElseGet(() -> {
        byteBufAllocator = Optional.of(new ByteBufAllocatorConfig());
        return byteBufAllocator.get();
      }));
      return this;
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  /**
   * The state of request timing metrics.
   *
   * @return True if request timing metrics is enabled. False otherwise
   * @since 1.2
   */
  public boolean isRequestTimingMetrics() {
    return requestTimingMetrics;
  }

  /**
   * The state of request timing metrics reporting.
   * @param requestTimingMetrics True if request timing metrics are to be reported. False otherwise
   * @return this
   * @since 1.2
   */
  public DropwizardMetricsConfig requestTimingMetrics(boolean requestTimingMetrics) {
    this.requestTimingMetrics = requestTimingMetrics;
    return this;
  }

  /**
   * The state of blocking timing metrics.
   *
   * @return True if blocking timing metrics is enabled. False otherwise
   * @since 1.2
   */
  public boolean isBlockingTimingMetrics() {
    return blockingTimingMetrics;
  }

  /**
   * The state of blocking timing metrics reporting.
   * @param blockingTimingMetrics True if blocking timing metrics are to be reported. False otherwise
   * @return this
   * @since 1.2
   */
  public DropwizardMetricsConfig blockingTimingMetrics(boolean blockingTimingMetrics) {
    this.blockingTimingMetrics = blockingTimingMetrics;
    return this;
  }

  /**
   * A map of regular expressions used to group request metrics.
   * <p>
   * The value is a regular expression to test the current request path against for a match.
   * If matched, the key is the name to use when recording the metric.  Please note that request
   * paths do not start with a <code>/</code>
   * <p>
   * As soon as a match is made against a regular expression no further matches are attempted.
   * <p>
   * Should no matches be made the default metric grouping will be used.
   *
   * @return the request metric group expressions
   * @see DefaultRequestTimingHandler
   */
  public Map<String, String> getRequestMetricGroups() {
    return requestMetricGroups;
  }

  /**
   * Configure the request metric groups.
   * @param requestMetricGroups the request metric groups
   * @return this
   */
  public DropwizardMetricsConfig requestMetricGroups(Map<String, String> requestMetricGroups) {
    this.requestMetricGroups = requestMetricGroups;
    return this;
  }

  /**
   * Get the settings for the JMX metrics publisher.
   * @return the jmx publisher settings
   */
  public Optional<JmxConfig> getJmx() {
    return jmx;
  }

  /**
   * @see #jmx(ratpack.func.Action)
   * @return this
   */
  public DropwizardMetricsConfig jmx() {
    return jmx(Action.noop());
  }

  /**
   * Configure the JMX metrics publisher.
   *
   * @param configure the configuration for the publisher
   * @return this
   */
  public DropwizardMetricsConfig jmx(Action<? super JmxConfig> configure) {
    try {
      configure.execute(jmx.orElseGet(() -> {
        jmx = Optional.of(new JmxConfig());
        return jmx.get();
      }));
      return this;
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  /**
   * Get the settings for the console metrics publisher.
   * @return the console publisher settings
   */
  public Optional<ConsoleConfig> getConsole() {
    return console;
  }

  /**
   * @see #console(ratpack.func.Action)
   * @return this
   */
  public DropwizardMetricsConfig console() {
    return console(Action.noop());
  }

  /**
   * Configure the console metrics publisher.
   *
   * @param configure the configuration for the publisher
   * @return this
   */
  public DropwizardMetricsConfig console(Action<? super ConsoleConfig> configure) {
    try {
      configure.execute(console.orElseGet(() -> {
        console = Optional.of(new ConsoleConfig());
        return console.get();
      }));
      return this;
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  /**
   * Get the settings for the Slf4j Logger metrics publisher.
   * @return the slf4j publisher settings
   */
  public Optional<Slf4jConfig> getSlf4j() {
    return slf4j;
  }

  /**
   * @see #slf4j(ratpack.func.Action)
   * @return this
   */
  public DropwizardMetricsConfig slf4j() {
    return slf4j(Action.noop());
  }

  /**
   * Configure the Slf4j logger metrics publisher.
   *
   * @param configure the configuration for the publisher
   * @return this
   */
  public DropwizardMetricsConfig slf4j(Action<? super Slf4jConfig> configure) {
    try {
      configure.execute(slf4j.orElseGet(() -> {
        slf4j = Optional.of(new Slf4jConfig());
        return slf4j.get();
      }));
      return this;
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  /**
   * Get the settings for the websockets metrics broadcaster.
   * @return the websockets broadcaster settings
   */
  public Optional<WebsocketConfig> getWebSocket() {
    return webSocket;
  }

  /**
   * @see #webSocket(ratpack.func.Action)
   * @return this
   */
  public DropwizardMetricsConfig webSocket() {
    return webSocket(Action.noop());
  }

  /**
   * Configure the websockets metrics broadcaster.
   *
   * @param configure the configuration for the broadcaster
   * @return this
   */
  public DropwizardMetricsConfig webSocket(Action<? super WebsocketConfig> configure) {
    try {
      configure.execute(webSocket.orElseGet(() -> {
        webSocket = Optional.of(new WebsocketConfig());
        return webSocket.get();
      }));
      return this;
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  /**
   * Get the settings for the csv metrics publisher.
   * @return the csv publisher settings
   */
  public Optional<CsvConfig> getCsv() {
    return csv;
  }

  /**
   * Configure the csv metrics publisher.
   *
   * @param configure the configuration for the publisher
   * @return this
   */
  public DropwizardMetricsConfig csv(Action<? super CsvConfig> configure) {
    try {
      configure.execute(csv.orElseGet(() -> {
        csv = Optional.of(new CsvConfig());
        return csv.get();
      }));
      return this;
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  /**
   * Get the settings for the Graphite metrics publisher.
   * @return the Graphite publisher settings
   */
  public Optional<GraphiteConfig> getGraphite() {
    return graphite;
  }

  /**
   * Configure the Graphite metrics publisher.
   *
   * @param configure the configuration for the publisher
   * @return this
   */
  public DropwizardMetricsConfig graphite(Action<? super GraphiteConfig> configure) {
    try {
      configure.execute(graphite.orElseGet(() -> {
        graphite = Optional.of(new GraphiteConfig());
        return graphite.get();
      }));
      return this;
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  /**
   * Get the settings for the http client metric set.
   *
   * @return the metric set settings.
   * @since 1.6
   */
  public Optional<HttpClientConfig> getHttpClient() {
    return httpClient;
  }

  /**
   * @return this
   * @see #httpClient(ratpack.func.Action)
   * @since 1.6
   */
  public DropwizardMetricsConfig httpClient() {
    return httpClient(Action.noop());
  }

  /**
   * Configure the http client metric set.
   *
   * @param configure the configuration for the http client metric set.
   * @return this
   * @since 1.6
   */
  public DropwizardMetricsConfig httpClient(Action<? super HttpClientConfig> configure) {
    try {
      configure.execute(httpClient.orElseGet(() -> {
        httpClient = Optional.of(new HttpClientConfig());
        return httpClient.get();
      }));
      return this;
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

}
