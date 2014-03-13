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

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
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
 *   modules {
 *     register new CodaHaleMetricsModule().jmx()
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
 *   modules {
 *     register new CodaHaleMetricsModule().jmx().console()
 *   }
 * }
 * </pre>
 * <p>
 * This module supports both metric collection and health checks.  For further details on both please see
 * {@link ratpack.codahale.metrics.CodaHaleMetricsModule#metrics()} and {@link ratpack.codahale.metrics.CodaHaleMetricsModule#healthChecks()}
 * respectively.  By default metric collection is not enabled but health checks are.
 * </p>
 * <p>
 * <b>It is important that this module is registered first in the modules list to ensure that request metrics are as accurate as possible.</b>
 * </p>
 *
 * @see <a href="http://metrics.codahale.com/" target="_blank">Coda Hale's Metrics</a>
 */
public class CodaHaleMetricsModule extends AbstractModule implements HandlerDecoratingModule {

  private boolean reportMetricsToJmx;
  private boolean reportMetricsToConsole;
  private File csvReportDirectory;
  private boolean healthChecksEnabled = true;
  private boolean jvmMetricsEnabled;
  private boolean reportMetricsToWebsocket;
  private boolean metricsEnabled;

  private boolean isMetricsEnabled() {
    return metricsEnabled || jvmMetricsEnabled || reportMetricsToConsole || reportMetricsToWebsocket || reportMetricsToJmx || csvReportDirectory != null;
  }

  @Override
  protected void configure() {
    if (isMetricsEnabled()) {
      final MetricRegistry metricRegistry = new MetricRegistry();
      bind(MetricRegistry.class).toInstance(metricRegistry);
      bind(BackgroundProcessingTimingInterceptor.class);

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
        bind(MetricsBroadcaster.class).in(Singleton.class);
        bind(WebSocketReporter.class).asEagerSingleton();
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
   * <p>
   * To enable one of the built in metric reporters please chain the relevant reporter configuration
   * e.g. {@link ratpack.codahale.metrics.CodaHaleMetricsModule#jmx()}, {@link ratpack.codahale.metrics.CodaHaleMetricsModule#console()}.
   * </p>
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
   *   modules {
   *     register new CodaHaleMetricsModule().jmx()
   *   }
   *
   *   handlers { MetricRegistry metricRegistry ->
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
   *
   * @return this {@code CodaHaleMetricsModule}
   * @see <a href="http://metrics.codahale.com/getting-started/" target="_blank">Coda Hale Metrics - Getting Started</a>
   * @see ratpack.codahale.metrics.CodaHaleMetricsModule#jmx()
   * @see ratpack.codahale.metrics.CodaHaleMetricsModule#console()
   * @see CodaHaleMetricsModule#csv(java.io.File)
   */
  public CodaHaleMetricsModule metrics() {
    return metrics(true);
  }

  /**
   * Enables or disables the collecting of metrics.
   *
   * @param enabled If the metric collection should be enabled.
   * @return this {@code CodaHaleMetricsModule}
   * @see ratpack.codahale.metrics.CodaHaleMetricsModule#metrics()
   */
  public CodaHaleMetricsModule metrics(boolean enabled) {
    this.metricsEnabled = enabled;
    return this;
  }

  /**
   * Enables the automatic registering of health checks.
   * <p>
   * Health checks verify that application components or responsibilities are performing as expected.
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
   *   modules {
   *     register new CodaHaleMetricsModule().healthChecks()
   *     bind FooHealthCheck // if you don't bind the health check with Guice it will not be automatically registered
   *   }
   *
   *   handlers {
   *     // Using the provided handler…
   *     get("health-check/:name", new HealthCheckHandler())
   *
   *     // Using a custom handler to run all health checks…
   *     get("healthChecks-custom") { HealthCheckRegistry healthCheckRegistry ->
   *       render healthCheckRegistry.runHealthChecks().toString()
   *     }
   *   }
   * }
   * </pre>
   *
   * @return this {@code CodaHaleMetricsModule}
   * @see <a href="http://metrics.codahale.com/manual/healthchecks/" target="_blank">Coda Hale Metrics - Health Checks</a>
   * @see HealthCheckHandler
   * @see com.codahale.metrics.health.HealthCheckRegistry#runHealthChecks()
   * @see HealthCheckRegistry#runHealthCheck(String)
   * @see HealthCheckRegistry#runHealthChecks(java.util.concurrent.ExecutorService)
   */
  public CodaHaleMetricsModule healthChecks() {
    return healthChecks(true);
  }

  /**
   * Enables or disables the automatic registering of health checks.
   *
   * @param enabled If the automatic registering of health checks should be enabled.
   * @return this {@code CodaHaleMetricsModule}
   * @see CodaHaleMetricsModule#healthChecks()
   */
  public CodaHaleMetricsModule healthChecks(boolean enabled) {
    this.healthChecksEnabled = enabled;
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
    return jvmMetrics(true);
  }

  /**
   * Enables or disables the collecting of JVM metrics.
   *
   * @param enabled If JVM metric collection should be enabled.
   * @return this {@code CodaHaleMetricsModule}
   * @see CodaHaleMetricsModule#jvmMetrics()
   */
  public CodaHaleMetricsModule jvmMetrics(boolean enabled) {
    this.jvmMetricsEnabled = enabled;
    return this;
  }

  /**
   * Enable the reporting of metrics via web sockets.  The collecting of metrics will also be enabled.
   * <p>
   * To broadcast metrics within an application see {@link MetricsHandler}.
   *
   * @return this {@code CodaHaleMetricsModule}
   * @see #console()
   * @see #csv(java.io.File)
   * @see #jmx()
   */
  public CodaHaleMetricsModule websocket() {
    return websocket(true);
  }

  /**
   * Enables or disables the reporting of metrics via web sockets.
   *
   * @param enabled If web socket metric reporting should be enabled.
   * @return this {@code CodaHaleMetricsModule}
   * @see #websocket()
   */
  public CodaHaleMetricsModule websocket(boolean enabled) {
    this.reportMetricsToWebsocket = enabled;
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
    return jmx(true);
  }

  /**
   * Enables or disables the reporting of metrics via JMX.
   *
   * @param enabled If JMX metric reporting should be enabled.
   * @return this {@code CodaHaleMetricsModule}
   * @see #jmx()
   */
  public CodaHaleMetricsModule jmx(boolean enabled) {
    this.reportMetricsToJmx = enabled;
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
    return console(true);
  }

  /**
   * Enables or disables the reporting of metrics to the Console.
   *
   * @param enabled If Console metric reporting should be enabled.
   * @return this {@code CodaHaleMetricsModule}
   * @see #console()
   */
  public CodaHaleMetricsModule console(boolean enabled) {
    this.reportMetricsToConsole = enabled;
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
      GuiceUtil.eachOfType(injector, TypeLiteral.get(NamedHealthCheck.class), new Action<NamedHealthCheck>() {
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
