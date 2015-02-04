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
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.google.inject.Injector;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import ratpack.codahale.metrics.internal.*;
import ratpack.func.Action;
import ratpack.guice.ConfigurableModule;
import ratpack.handling.HandlerDecorator;
import ratpack.server.Service;
import ratpack.server.StartEvent;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.File;
import java.time.Duration;

import static com.google.inject.Scopes.SINGLETON;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ratpack.util.ExceptionUtils.uncheck;

/**
 * An extension module that provides support for Coda Hale's Metrics.
 * <p>
 * To use it one has to register the module and enable the required functionality by chaining the various configuration
 * options.  For example, to enable the capturing and reporting of metrics to {@link ratpack.codahale.metrics.CodaHaleMetricsModule.Config#jmx(ratpack.func.Action)}
 * one would write: (Groovy DSL)
 * </p>
 * <pre class="groovy-ratpack-dsl">
 * import ratpack.codahale.metrics.CodaHaleMetricsModule
 * import static ratpack.groovy.Groovy.ratpack
 *
 * ratpack {
 *   bindings {
 *     add new CodaHaleMetricsModule(), { it.enable(true).jmx { it.enable(true) } }
 *   }
 * }
 * </pre>
 * <p>
 * To enable the capturing and reporting of metrics to JMX and the console, one would
 * write: (Groovy DSL)
 * </p>
 * <pre class="groovy-ratpack-dsl">
 * import ratpack.codahale.metrics.CodaHaleMetricsModule
 * import static ratpack.groovy.Groovy.ratpack
 *
 * ratpack {
 *   bindings {
 *     add new CodaHaleMetricsModule(), { it.enable(true).jmx { it.enable(true) }.console { it.enable(true) } }
 *   }
 * }
 * </pre>
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
 *     add new CodaHaleMetricsModule(), { it.enable(true).jmx { it.enable(true) } }
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
 *
 * @see <a href="http://metrics.codahale.com/" target="_blank">Coda Hale's Metrics</a>
 */
public class CodaHaleMetricsModule extends ConfigurableModule<CodaHaleMetricsModule.Config> {

  public static final String RATPACK_METRIC_REGISTRY = "ratpack-metrics";

  /**
   * The configuration object for {@link CodaHaleMetricsModule}.
   */
  public static class Config {
    public static final Duration DEFAULT_INTERVAL = Duration.ofSeconds(30);

    private boolean enabled;
    private boolean jvmMetrics;

    private Jmx jmx = new Jmx();
    private Console console = new Console();
    private WebSocket webSocket = new WebSocket();
    private Csv csv = new Csv();

    /**
     * The state of metric collection.
     *
     * @return True if metrics collection is enabled. False otherwise
     */
    public boolean isEnabled() {
      return enabled;
    }

    /**
     * Enables the collection of metrics.
     *
     * @param enabled whether metrics collection should be enabled
     * @return this
     * @see <a href="http://metrics.codahale.com/getting-started/" target="_blank">Coda Hale Metrics - Getting Started</a>
     */
    public Config enable(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

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
     * @param jvmMetrics True is JVM metrics are report. False otherwise
     * @return this
     */
    public Config jvmMetrics(boolean jvmMetrics) {
      this.jvmMetrics = jvmMetrics;
      return this;
    }

    /**
     * Get the settings for the JMX metrics publisher.
     * @return the jmx publisher settings
     */
    public Jmx getJmx() {
      return jmx;
    }

    /**
     * Configure the JMX metrics publisher.
     *
     * @param configure the configuration for the publisher
     * @return this
     */
    public Config jmx(Action<Jmx> configure) {
      try {
        configure.execute(jmx);
        return this;
      } catch (Exception e) {
        throw uncheck(e);
      }
    }

    /**
     * Get the settings for the console metrics publisher.
     * @return the console publisher settings
     */
    public Console getConsole() {
      return console;
    }

    /**
     * Configure the console metrics publisher.
     *
     * @param configure the configuration for the publisher
     * @return this
     */
    public Config console(Action<Console> configure) {
      try {
        configure.execute(console);
        return this;
      } catch (Exception e) {
        throw uncheck(e);
      }
    }

    /**
     * Get the settings for the websockets metrics broadcaster.
     * @return the websockets broadcaster settings
     */
    public WebSocket getWebSocket() {
      return webSocket;
    }

    /**
     * Configure the websockets metrics broadcaster.
     *
     * @param configure the configuration for the broadcaster
     * @return this
     */
    public Config webSocket(Action<WebSocket> configure) {
      try {
        configure.execute(webSocket);
        return this;
      } catch (Exception e) {
        throw uncheck(e);
      }
    }

    /**
     * Get the settings for the csv metrics publisher.
     * @return the csv publisher settings
     */
    public Csv getCsv() {
      return csv;
    }

    /**
     * Configure the csv metrics publisher.
     *
     * @param configure the configuration for the publisher
     * @return this
     */
    public Config csv(Action<Csv> configure) {
      try {
        configure.execute(csv);
        return this;
      } catch (Exception e) {
        throw uncheck(e);
      }
    }

    public static class Jmx {
      private boolean enabled;

      /**
       * The state of the JMX publisher.
       * @return the state of the JMX publisher
       */
      public boolean isEnabled() {
        return enabled;
      }

      /**
       * Set the state of the JMX publisher.
       * @param enabled True if metrics are published to JMX. False otherwise
       * @return this
       */
      public Jmx enable(boolean enabled) {
        this.enabled = enabled;
        return this;
      }
    }

    public static class Console {
      private boolean enabled;
      private Duration reporterInterval = DEFAULT_INTERVAL;

      /**
       * The state of the console publisher.
       * @return the state of the console publisher
       */
      public boolean isEnabled() {
        return enabled;
      }

      /**
       * Set the state of the console publisher.
       * @param enabled True if metrics are published to the console. False otherwise
       * @return this
       */
      public Console enable(boolean enabled) {
        this.enabled = enabled;
        return this;
      }

      /**
       * The interval between metrics reports.
       * @return the interval between metrics reports
       */
      public Duration getReporterInterval() {
        return reporterInterval;
      }

      /**
       * Configure the interval between metrics reports.
       *
       * @param reporterInterval the report interval
       * @return this
       */
      public Console reporterInterval(Duration reporterInterval) {
        this.reporterInterval = reporterInterval;
        return this;
      }
    }

    public static class WebSocket {
      private Duration reporterInterval = DEFAULT_INTERVAL;

      /**
       * The interval between metrics reports.
       * @return the interval between metrics reports
       */
      public Duration getReporterInterval() {
        return reporterInterval;
      }

      /**
       * Configure the interval between broadcasts.
       *
       * @param reporterInterval the report interval
       * @return this
       */
      public WebSocket reporterInterval(Duration reporterInterval) {
        this.reporterInterval = reporterInterval;
        return this;
      }
    }

    public static class Csv {
      private boolean enabled;
      private Duration reporterInterval = DEFAULT_INTERVAL;
      private File reportDirectory;

      /**
       * The state of the csv publisher.
       * @return True if published is enabled and report directory is specified. False otherwise
       */
      public boolean isEnabled() {
        return enabled && reportDirectory != null;
      }

      /**
       * Set the state of the csv publisher.
       * @param enabled True if metrics are published a csv file. False otherwise
       * @return this
       */
      public Csv enable(boolean enabled) {
        this.enabled = enabled;
        return this;
      }

      /**
       * The interval between metrics reports.
       * @return the interval between metrics reports
       */
      public Duration getReporterInterval() {
        return reporterInterval;
      }

      /**
       * Configure the interval between metrics reports.
       *
       * @param reporterInterval the report interval
       * @return this
       */
      public Csv reporterInterval(Duration reporterInterval) {
        this.reporterInterval = reporterInterval;
        return this;
      }

      /**
       * The directory to output CSV metrics reports to.
       * @return the output directory
       */
      public File getReportDirectory() {
        return reportDirectory;
      }

      /**
       * Configure the output directory for csv metrics reports.
       * @param reportDirectory The directory to place csv metrics reports
       * @return this
       */
      public Csv reportDirectory(File reportDirectory) {
        this.reportDirectory = reportDirectory;
        return this;
      }
    }

  }

  @Override
  protected void configure() {
    SharedMetricRegistries.remove(RATPACK_METRIC_REGISTRY);
    MetricRegistry metricRegistry = SharedMetricRegistries.getOrCreate(RATPACK_METRIC_REGISTRY);
    bind(MetricRegistry.class).toInstance(metricRegistry);

    bindInterceptor(Matchers.any(), Matchers.annotatedWith(Metered.class), injected(new MeteredMethodInterceptor()));
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(Timed.class), injected(new TimedMethodInterceptor()));
    bindListener(Matchers.any(), injected(new GaugeTypeListener()));

    bind(JmxReporter.class).toProvider(JmxReporterProvider.class).in(SINGLETON);
    bind(ConsoleReporter.class).toProvider(ConsoleReporterProvider.class).in(SINGLETON);
    bind(CsvReporter.class).toProvider(CsvReporterProvider.class).in(SINGLETON);
    bind(MetricRegistryPeriodicPublisher.class).in(SINGLETON);
    bind(MetricsBroadcaster.class).in(SINGLETON);
    bind(MetricRegistryJsonMapper.class).in(SINGLETON);

    bind(Startup.class);
    Multibinder.newSetBinder(binder(), HandlerDecorator.class).addBinding().toProvider(HandlerDecoratorProvider.class);
  }

  private <T> T injected(T instance) {
    requestInjection(instance);
    return instance;
  }

  private static class Startup implements Service {

    private final Config config;
    private final Injector injector;

    @Inject
    public Startup(Config config, Injector injector) {
      this.config = config;
      this.injector = injector;
    }

    @Override
    public void onStart(StartEvent event) throws Exception {
      if (config.isEnabled()) {
        Config.Jmx jmx = config.getJmx();
        Config.Console console = config.getConsole();
        Config.Csv csv = config.getCsv();
        if (jmx.isEnabled()) {
          injector.getInstance(JmxReporter.class).start();
        }
        if (console.isEnabled()) {
          injector.getInstance(ConsoleReporter.class).start(console.getReporterInterval().getSeconds(), SECONDS);
        }
        if (csv.isEnabled()) {
          injector.getInstance(CsvReporter.class).start(csv.getReporterInterval().getSeconds(), SECONDS);
        }
        if (config.isJvmMetrics()) {
          final MetricRegistry metricRegistry = injector.getInstance(MetricRegistry.class);
          metricRegistry.registerAll(new GarbageCollectorMetricSet());
          metricRegistry.registerAll(new ThreadStatesGaugeSet());
          metricRegistry.registerAll(new MemoryUsageGaugeSet());
        }
      }
    }
  }

  private static class HandlerDecoratorProvider implements Provider<HandlerDecorator> {
    private final Config config;

    @Inject
    public HandlerDecoratorProvider(Config config) {
      this.config = config;
    }

    @Override
    public HandlerDecorator get() {
      return config.isEnabled() ? HandlerDecorator.prepend(new RequestTimingHandler()) : HandlerDecorator.passThru();
    }
  }

}
