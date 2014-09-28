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

package ratpack.codahale.metrics;

import com.codahale.metrics.*;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.google.common.reflect.TypeToken;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;
import ratpack.codahale.metrics.internal.*;
import ratpack.func.Action;
import ratpack.guice.HandlerDecoratingModule;
import ratpack.guice.internal.GuiceUtil;
import ratpack.handling.Handler;

import java.io.File;

/**
 * An extension module that provides support for Coda Hale's Metrics.
 * <p>
 * To use it one has to register the module and enable the required functionality by chaining the various configuration
 * options.  For example, to enable the capturing and reporting of metrics to {@link ratpack.codahale.metrics.CodaHaleMetricsModule#jmx()}
 * one would write: (Groovy DSL)
 * </p>
 * <pre class="groovy-ratpack-dsl">
 * import ratpack.codahale.metrics.CodaHaleMetricsModule
 * import static ratpack.groovy.Groovy.ratpack
 *
 * ratpack {
 *   bindings {
 *     add new CodaHaleMetricsModule().jmx()
 *   }
 * }
 * </pre>
 * <p>
 * To enable the capturing and reporting of metrics to JMX and the {@link ratpack.codahale.metrics.CodaHaleMetricsModule#console()}, one would
 * write: (Groovy DSL)
 * </p>
 * <pre class="groovy-ratpack-dsl">
 * import ratpack.codahale.metrics.CodaHaleMetricsModule
 * import static ratpack.groovy.Groovy.ratpack
 *
 * ratpack {
 *   bindings {
 *     add new CodaHaleMetricsModule().jmx().console()
 *   }
 * }
 * </pre>
 * <p>
 * This module supports both metric collection and health checks.
 * </p>
 * <h3>Metrics</h3>
 * <p>
 * By default {@link com.codahale.metrics.Timer} metrics are collected for all requests received.  The module adds a
 * {@link RequestTimingHandler} to the handler chain <b>before</b> any user handlers.  This means that response times do not take any
 * framework overhead into account and purely the amount of time spent in handlers.  It is important that the module is
 * registered first in the modules list to ensure that <b>all</b> handlers are included in the metric.
 * </p>
 * <p>
 * Additional custom metrics can be registered with the provided {@link MetricRegistry} instance
 * </p>
 * <p>
 * Example custom metrics: (Groovy DSL)
 * </p>
 * <pre class="groovy-ratpack-dsl">
 * import ratpack.codahale.metrics.CodaHaleMetricsModule
 * import com.codahale.metrics.MetricRegistry
 * import static ratpack.groovy.Groovy.ratpack
 *
 * ratpack {
 *   bindings {
 *     add new CodaHaleMetricsModule().jmx()
 *   }
 *
 *   handlers { MetricRegistry metricRegistry -&gt;
 *     handler {
 *       metricRegistry.meter("my custom meter").mark()
 *       render ""
 *     }
 *   }
 * }
 * </pre>
 * <p>
 * Custom metrics can also be added via the Metrics annotations ({@link Metered}, {@link Timed} and {@link com.codahale.metrics.annotation.Gauge})
 * to any Guice injected classes.
 * </p>
 * <h3>Health checks</h3>
 * <p>
 * Health checks verify that application components or responsibilities are performing as expected.
 * </p>
 * <p>
 * To create a health check simply create a class that extends {@link NamedHealthCheck} and bind it with Guice.
 * This will automatically add it to the application wide {@link HealthCheckRegistry}.
 * <p>
 * Health checks can be run by obtaining the {@link HealthCheckRegistry} via dependency injection or context registry lookup,
 * then calling {@link HealthCheckRegistry#runHealthChecks()}.
 * <p>
 * To expose the health check status over HTTP, see {@link HealthCheckHandler}.
 * <p>
 * Example health checks: (Groovy DSL)
 * </p>
 * <pre class="groovy-ratpack-dsl">
 * import com.codahale.metrics.health.HealthCheck
 * import com.codahale.metrics.health.HealthCheckRegistry
 * import ratpack.codahale.metrics.CodaHaleMetricsModule
 * import ratpack.codahale.metrics.HealthCheckHandler
 * import ratpack.codahale.metrics.NamedHealthCheck
 * import static ratpack.groovy.Groovy.ratpack
 *
 * class FooHealthCheck extends NamedHealthCheck {
 *
 *   protected HealthCheck.Result check() throws Exception {
 *     // perform the health check logic here and return HealthCheck.Result.healthy() or HealthCheck.Result.unhealthy("Unhealthy message")
 *     HealthCheck.Result.healthy()
 *   }
 *
 *   def String getName() {
 *     "foo_health_check"
 *   }
 * }
 *
 * ratpack {
 *   bindings {
 *     add new CodaHaleMetricsModule().healthChecks()
 *     bind FooHealthCheck // if you don't bind the health check with Guice it will not be automatically registered
 *   }
 *
 *   handlers {
 *     // Using the provided handler…
 *     get("health-check/:name", new HealthCheckHandler())
 *
 *     // Using a custom handler to run all health checks…
 *     get("healthChecks-custom") { HealthCheckRegistry healthCheckRegistry -&gt;
 *       render healthCheckRegistry.runHealthChecks().toString()
 *     }
 *   }
 * }
 * </pre>
 *
 * @see <a href="http://metrics.codahale.com/" target="_blank">Coda Hale's Metrics</a>
 * @see <a href="http://metrics.codahale.com/manual/healthchecks/" target="_blank">Coda Hale Metrics - Health Checks</a>
 */
public class CodaHaleMetricsModule extends AbstractModule implements HandlerDecoratingModule {

  public static final String RATPACK_METRIC_REGISTRY = "ratpack-metrics";
  private boolean reportMetricsToJmx;
  private boolean reportMetricsToConsole;
  private File csvReportDirectory;
  private boolean healthChecksEnabled;
  private boolean jvmMetricsEnabled;
  private boolean reportMetricsToWebsocket;
  private boolean metricsEnabled;

  private boolean isMetricsEnabled() {
    return metricsEnabled || jvmMetricsEnabled || reportMetricsToConsole || reportMetricsToWebsocket || reportMetricsToJmx || csvReportDirectory != null;
  }

  @Override
  protected void configure() {
    if (isMetricsEnabled()) {
      SharedMetricRegistries.remove(RATPACK_METRIC_REGISTRY);
      final MetricRegistry metricRegistry = SharedMetricRegistries.getOrCreate(RATPACK_METRIC_REGISTRY);
      bind(MetricRegistry.class).toInstance(metricRegistry);

      MeteredMethodInterceptor meteredMethodInterceptor = new MeteredMethodInterceptor();
      requestInjection(meteredMethodInterceptor);
      bindInterceptor(Matchers.any(), Matchers.annotatedWith(Metered.class), meteredMethodInterceptor);

      TimedMethodInterceptor timedMethodInterceptor = new TimedMethodInterceptor();
      requestInjection(timedMethodInterceptor);
      bindInterceptor(Matchers.any(), Matchers.annotatedWith(Timed.class), timedMethodInterceptor);

      GaugeTypeListener gaugeTypeListener = new GaugeTypeListener(metricRegistry);
      bindListener(Matchers.any(), gaugeTypeListener);

      if (reportMetricsToJmx) {
        bind(JmxReporter.class).toProvider(JmxReporterProvider.class).asEagerSingleton();
      }

      if (reportMetricsToConsole) {
        bind(ConsoleReporter.class).toProvider(ConsoleReporterProvider.class).asEagerSingleton();
      }

      if (csvReportDirectory != null) {
        bind(File.class).annotatedWith(Names.named(CsvReporterProvider.CSV_REPORT_DIRECTORY)).toInstance(csvReportDirectory);
        bind(CsvReporter.class).toProvider(CsvReporterProvider.class).asEagerSingleton();
      }

      if (reportMetricsToWebsocket) {
        bind(MetricRegistryPeriodicPublisher.class).in(Singleton.class);
        bind(MetricsBroadcaster.class).in(Singleton.class);
        bind(MetricRegistryJsonMapper.class).in(Singleton.class);
      }
    }

    if (healthChecksEnabled) {
      bind(HealthCheckRegistry.class).in(Singleton.class);
    }

    bind(HealthCheckResultRenderer.class).in(Singleton.class);
    bind(HealthCheckResultsRenderer.class).in(Singleton.class);
  }

  /**
   * Enables the collection of metrics.
   *
   * @return this {@code CodaHaleMetricsModule}
   * @see <a href="http://metrics.codahale.com/getting-started/" target="_blank">Coda Hale Metrics - Getting Started</a>
   * @see #jmx()
   * @see #console()
   * @see #csv(java.io.File)
   * @see #websocket()
   */
  public CodaHaleMetricsModule metrics() {
    this.metricsEnabled = true;
    return this;
  }

  /**
   * Enables the automatic registering of health checks.
   *
   * @return this {@code CodaHaleMetricsModule}
   * @see <a href="http://metrics.codahale.com/manual/healthchecks/" target="_blank">Coda Hale Metrics - Health Checks</a>
   * @see HealthCheckHandler
   * @see com.codahale.metrics.health.HealthCheckRegistry#runHealthChecks()
   * @see HealthCheckRegistry#runHealthCheck(String)
   * @see HealthCheckRegistry#runHealthChecks(java.util.concurrent.ExecutorService)
   */
  public CodaHaleMetricsModule healthChecks() {
    this.healthChecksEnabled = true;
    return this;
  }

  /**
   * Enable the collection of JVM metrics.
   * <p>
   * The JVM Gauges and Metric Sets provided by Coda Hale's Metrics will be registered to this module's Metric Registry.
   *
   * @return this {@code CodaHaleMetricsModule}
   * @see <a href="http://metrics.codahale.com/manual/jvm/" target="_blank">Coda Hale Metrics - JVM Instrumentation</a>
   */
  public CodaHaleMetricsModule jvmMetrics() {
    this.jvmMetricsEnabled = true;
    return this;
  }

  /**
   * Enable the reporting of metrics via web sockets.  The collecting of metrics will also be enabled.
   * <p>
   * To broadcast metrics within an application see {@link MetricsWebsocketBroadcastHandler}.
   *
   * @return this {@code CodaHaleMetricsModule}
   * @see #console()
   * @see #csv(java.io.File)
   * @see #jmx()
   */
  public CodaHaleMetricsModule websocket() {
    this.reportMetricsToWebsocket = true;
    return this;
  }

  /**
   * Enable the reporting of metrics via JMX.  The collecting of metrics will also be enabled.
   *
   * @return this {@code CodaHaleMetricsModule}
   * @see <a href="http://metrics.codahale.com/getting-started/#reporting-via-jmx" target="_blank">Coda Hale Metrics - Reporting Via JMX</a>
   * @see #console()
   * @see #csv(java.io.File)
   * @see #websocket()
   */
  public CodaHaleMetricsModule jmx() {
    this.reportMetricsToJmx = true;
    return this;
  }

  /**
   * Enable the reporting of metrics to the Console.  The collecting of metrics will also be enabled.
   *
   * @return this {@code CodaHaleMetricsModule}
   * @see <a href="http://metrics.codahale.com/manual/core/#man-core-reporters-console" target="_blank">Coda Hale Metrics - Console Reporting</a>
   * @see #jmx()
   * @see #csv(java.io.File)
   * @see #websocket()
   */
  public CodaHaleMetricsModule console() {
    this.reportMetricsToConsole = true;
    return this;
  }

  /**
   * Enable the reporting of metrics to a CSV file.  The collecting of metrics will also be enabled.
   *
   * @param reportDirectory The directory in which to create the CSV report files.
   * @return this {@code CodaHaleMetricsModule}
   * @see <a href="http://metrics.codahale.com/manual/core/#man-core-reporters-csv" target="_blank">Coda Hale Metrics - CSV Reporting</a>
   * @see #jmx()
   * @see #console()
   * @see #websocket()
   */
  public CodaHaleMetricsModule csv(File reportDirectory) {
    if (reportDirectory == null) {
      throw new IllegalArgumentException("reportDirectory cannot be null");
    }

    csvReportDirectory = reportDirectory;
    return this;
  }

  @Override
  public Handler decorate(Injector injector, Handler handler) {
    if (healthChecksEnabled) {
      final HealthCheckRegistry registry = injector.getInstance(HealthCheckRegistry.class);
      GuiceUtil.eachOfType(injector, TypeToken.of(NamedHealthCheck.class), new Action<NamedHealthCheck>() {
        public void execute(NamedHealthCheck healthCheck) throws Exception {
          registry.register(healthCheck.getName(), healthCheck);
        }
      });
    }

    if (jvmMetricsEnabled) {
      final MetricRegistry metricRegistry = injector.getInstance(MetricRegistry.class);
      metricRegistry.registerAll(new GarbageCollectorMetricSet());
      metricRegistry.registerAll(new ThreadStatesGaugeSet());
      metricRegistry.registerAll(new MemoryUsageGaugeSet());
    }

    if (isMetricsEnabled()) {
      return new RequestTimingHandler(handler);
    } else {
      return handler;
    }
  }
}
