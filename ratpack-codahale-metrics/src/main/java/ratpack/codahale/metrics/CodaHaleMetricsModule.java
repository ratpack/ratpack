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
import com.codahale.metrics.Slf4jReporter.LoggingLevel;
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
import org.slf4j.Logger;
import org.slf4j.Marker;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.File;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.inject.Scopes.SINGLETON;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ratpack.util.Exceptions.uncheck;

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
 *     module new CodaHaleMetricsModule(), { it.jmx() }
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
 *     module new CodaHaleMetricsModule(), { it.jmx().console() }
 *   }
 * }
 * </pre>
 *
 * <h2>External Configuration</h2>
 * The module can also be configured via external configuration using the
 * <a href="http://www.ratpack.io/manual/current/api/ratpack/config/ConfigData.html" target="_blank">ratpack-config</a> extension.
 * For example, to enable the capturing and reporting of metrics to jmx via an external property file which can be overridden with
 * system properties one would write: (Groovy DSL)
 *
 * <pre class="groovy-ratpack-dsl">
 * import com.google.common.collect.ImmutableMap
 * import ratpack.codahale.metrics.CodaHaleMetricsModule
 * import ratpack.config.ConfigData
 * import static ratpack.groovy.Groovy.ratpack
 *
 * ratpack {
 *   bindings {
 *     ConfigData configData = ConfigData.of()
 *       .props(ImmutableMap.of("metrics.jmx.enabled", "true")) // for demo purposes we are using a map to easily see the properties being set
 *       .sysProps()
 *       .build()
 *
 *     moduleConfig(new CodaHaleMetricsModule(), configData.get("/metrics", CodaHaleMetricsModule.Config))
 *   }
 * }
 * </pre>
 *
 * <h2>Metric Collection</h2>
 * <p>
 * By default {@link com.codahale.metrics.Timer} metrics are collected for all requests received and {@link Counter} metrics for response codes.
 * The module adds a {@link RequestTimingHandler} to the handler chain <b>before</b> any user handlers.  This means that response times do not
 * take any framework overhead into account and purely the amount of time spent in handlers.  It is important that the module is registered first
 * in the modules list to ensure that <b>all</b> handlers are included in the metric.
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
 *     module new CodaHaleMetricsModule(), { it.jmx() }
 *   }
 *
 *   handlers { MetricRegistry metricRegistry -&gt;
 *     all {
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

    private boolean jvmMetrics;
    private Map<String, String> requestMetricGroups;
    private Optional<Jmx> jmx = Optional.empty();
    private Optional<Console> console = Optional.empty();
    private Optional<WebSocket> webSocket = Optional.empty();
    private Optional<Csv> csv = Optional.empty();
    private Optional<Slf4j> slf4j = Optional.empty();

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
     * @see RequestTimingHandler
     */
    public Map<String, String> getRequestMetricGroups() {
      return requestMetricGroups;
    }

    /**
     * Configure the request metric groups
     * @param requestMetricGroups the request metric groups
     * @return this
     */
    public Config requestMetricGroups(Map<String, String> requestMetricGroups) {
      this.requestMetricGroups = requestMetricGroups;
      return this;
    }

    /**
     * Get the settings for the JMX metrics publisher.
     * @return the jmx publisher settings
     */
    public Optional<Jmx> getJmx() {
      return jmx;
    }

    /**
     * @see #jmx(ratpack.func.Action)
     * @return this
     */
    public Config jmx() {
      return jmx(Action.noop());
    }

    /**
     * Configure the JMX metrics publisher.
     *
     * @param configure the configuration for the publisher
     * @return this
     */
    public Config jmx(Action<? super Jmx> configure) {
      try {
        configure.execute(jmx.orElseGet(() -> {
          jmx = Optional.of(new Jmx());
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
    public Optional<Console> getConsole() {
      return console;
    }

    /**
     * @see #console(ratpack.func.Action)
     * @return this
     */
    public Config console() {
      return console(Action.noop());
    }

    /**
     * Configure the console metrics publisher.
     *
     * @param configure the configuration for the publisher
     * @return this
     */
    public Config console(Action<? super Console> configure) {
      try {
        configure.execute(console.orElseGet(() -> {
          console = Optional.of(new Console());
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
    public Optional<Slf4j> getSlf4j() {
      return slf4j;
    }

    /**
     * @see #slf4j(ratpack.func.Action)
     * @return this
     */
    public Config slf4j() {
      return slf4j(Action.noop());
    }

    /**
     * Configure the Slf4j logger metrics publisher.
     *
     * @param configure the configuration for the publisher
     * @return this
     */
    public Config slf4j(Action<? super Slf4j> configure) {
      try {
        configure.execute(slf4j.orElseGet(() -> {
          slf4j = Optional.of(new Slf4j());
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
    public Optional<WebSocket> getWebSocket() {
      return webSocket;
    }

    /**
     * @see #webSocket(ratpack.func.Action)
     * @return this
     */
    public Config webSocket() {
      return webSocket(Action.noop());
    }

    /**
     * Configure the websockets metrics broadcaster.
     *
     * @param configure the configuration for the broadcaster
     * @return this
     */
    public Config webSocket(Action<? super WebSocket> configure) {
      try {
        configure.execute(webSocket.orElseGet(() -> {
          webSocket = Optional.of(new WebSocket());
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
    public Optional<Csv> getCsv() {
      return csv;
    }

    /**
     * Configure the csv metrics publisher.
     *
     * @param configure the configuration for the publisher
     * @return this
     */
    public Config csv(Action<? super Csv> configure) {
      try {
        configure.execute(csv.orElseGet(() -> {
          csv = Optional.of(new Csv());
          return csv.get();
        }));
        return this;
      } catch (Exception e) {
        throw uncheck(e);
      }
    }

    public static class Jmx {
      private boolean enabled = true;
      private String includeFilter;
      private String excludeFilter;

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

      /**
       * The include metric filter expression of the reporter.
       *
       * @return the include filter
       */
      public String getIncludeFilter() {
        return includeFilter;
      }

      /**
       * Set the include metric filter expression of the reporter.
       *
       * @param includeFilter the regular expression to match on.
       * @return {@code this}
       */
      public Jmx includeFilter(String includeFilter) {
        this.includeFilter = includeFilter;
        return this;
      }

      /**
       * The exclude metric filter expression of the reporter.
       *
       * @return the exclude filter
       */
      public String getExcludeFilter() {
        return excludeFilter;
      }

      /**
       * Set the exclude metric filter expression of the reporter.
       *
       * @param excludeFilter the regular expression to match on.
       * @return {@code this}
       */
      public Jmx excludeFilter(String excludeFilter) {
        this.excludeFilter = excludeFilter;
        return this;
      }
    }

    public static class Console {
      private Duration reporterInterval = DEFAULT_INTERVAL;
      private boolean enabled = true;
      private String includeFilter;
      private String excludeFilter;

      /**
       * The state of the Console publisher.
       *
       * @return the state of the Console publisher
       */
      public boolean isEnabled() {
        return enabled;
      }

      /**
       * Set the state of the Console publisher.
       *
       * @param enabled True if metrics are published to the console. False otherwise
       * @return {@code this}
       */
      public Console enable(boolean enabled) {
        this.enabled = enabled;
        return this;
      }

      /**
       * The include metric filter expression of the reporter.
       *
       * @return the include filter
       */
      public String getIncludeFilter() {
        return includeFilter;
      }

      /**
       * Set the include metric filter of the reporter.
       *
       * @param includeFilter the regular expression to match on.
       * @return {@code this}
       */
      public Console includeFilter(String includeFilter) {
        this.includeFilter = includeFilter;
        return this;
      }

      /**
       * The exclude metric filter expression of the reporter.
       *
       * @return the exclude filter
       */
      public String getExcludeFilter() {
        return excludeFilter;
      }

      /**
       * Set the exclude metric filter expression of the reporter.
       *
       * @param excludeFilter the regular expression to match on.
       * @return {@code this}
       */
      public Console excludeFilter(String excludeFilter) {
        this.excludeFilter = excludeFilter;
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

    public static class Slf4j {
      private Duration reporterInterval = DEFAULT_INTERVAL;
      private boolean enabled = true;
      private String includeFilter;
      private String excludeFilter;

      private Marker marker;
      private String prefix;
      private TimeUnit durationUnit;
      private TimeUnit rateUnit;
      private LoggingLevel logLevel;
      private Logger logger;

      /**
       * The state of the marker.
       *
       * @return the marker instance
       */
      public Marker getMarker() {
        return marker;
      }

      /**
       * Mark all logged metrics with the given marker.
       *
       * @param marker an SLF4J {@link Marker}
       * @return {@code this}
       */
      public Slf4j marker(Marker marker) {
        this.marker = marker;
        return this;
      }

      /**
       * The logger prefix.
       *
       * @return the prefix text
       */
      public String getPrefix() {
        return prefix;
      }

      /**
       * Prefix all metric names with the given string.
       *
       * @param prefix the prefix for all metric names
       * @return {@code this}
       */
      public Slf4j prefix(String prefix) {
        this.prefix = prefix;
        return this;
      }

      /**
       * The state of the duration time unit.
       *
       * @return the duration unit instance
       */
      public TimeUnit getDurationUnit() {
        return durationUnit;
      }
      /**
       * Convert durations to the given time unit.
       *
       * @param durationUnit a unit of time
       * @return {@code this}
       */
      public Slf4j durationUnit(TimeUnit durationUnit) {
        this.durationUnit = durationUnit;
        return this;
      }

      /**
       * The state of the rate time unit.
       *
       * @return the rate unit instance
       */
      public TimeUnit getRateUnit() {
        return rateUnit;
      }
      /**
       * Convert rates to the given time unit.
       *
       * @param rateUnit a unit of time
       * @return {@code this}
       */
      public Slf4j rateUnit(TimeUnit rateUnit) {
        this.rateUnit = rateUnit;
        return this;
      }

      /**
       * The state of the logging level.
       *
       * @return the log level instance
       */
      public LoggingLevel getLogLevel() {
        return logLevel;
      }

      /**
       * Use Logging Level when reporting.
       *
       * @param logLevel a (@link LoggingLevel}
       * @return {@code this}
       */
      public Slf4j logLevel(LoggingLevel logLevel) {
        this.logLevel = logLevel;
        return this;
      }

      /**
       * The state of the logger.
       *
       * @return the logger instance
       */
      public Logger getLogger() {
        return logger;
      }

      /**
       * Log metrics to the given logger.
       *
       * @param logger an SLF4J {@link Logger}
       * @return {@code this}
       */
      public Slf4j logger(Logger logger) {
        this.logger = logger;
        return this;
      }

      /**
       * The state of the Slf4j publisher.
       *
       * @return the state of the Console publisher
       */
      public boolean isEnabled() {
        return enabled;
      }

      /**
       * Set the state of the Slf4j publisher.
       *
       * @param enabled True if metrics are published to the logger. False otherwise
       * @return {@code this}
       */
      public Slf4j enable(boolean enabled) {
        this.enabled = enabled;
        return this;
      }

      /**
       * The include metric filter expression of the reporter.
       *
       * @return the include filter
       */
      public String getIncludeFilter() {
        return includeFilter;
      }

      /**
       * Set the include metric filter of the reporter.
       *
       * @param includeFilter the regular expression to match on.
       * @return {@code this}
       */
      public Slf4j includeFilter(String includeFilter) {
        this.includeFilter = includeFilter;
        return this;
      }

      /**
       * The exclude metric filter expression of the reporter.
       *
       * @return the exclude filter
       */
      public String getExcludeFilter() {
        return excludeFilter;
      }

      /**
       * Set the exclude metric filter expression of the reporter.
       *
       * @param excludeFilter the regular expression to match on.
       * @return {@code this}
       */
      public Slf4j excludeFilter(String excludeFilter) {
        this.excludeFilter = excludeFilter;
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
      public Slf4j reporterInterval(Duration reporterInterval) {
        this.reporterInterval = reporterInterval;
        return this;
      }
    }

    public static class WebSocket {
      private Duration reporterInterval = DEFAULT_INTERVAL;
      private String includeFilter;
      private String excludeFilter;

      /**
       * The interval between metrics reports.
       *
       * @return the interval between metrics reports
       */
      public Duration getReporterInterval() {
        return reporterInterval;
      }

      /**
       * Configure the interval between broadcasts.
       *
       * @param reporterInterval the report interval
       * @return {@code this}
       */
      public WebSocket reporterInterval(Duration reporterInterval) {
        this.reporterInterval = reporterInterval;
        return this;
      }

      /**
       * The include metric filter expression of the reporter.
       *
       * @return the include filter
       */
      public String getIncludeFilter() {
        return includeFilter;
      }

      /**
       * Set the include metric filter of the reporter.
       *
       * @param includeFilter the regular expression to match on.
       * @return {@code this}
       */
      public WebSocket includeFilter(String includeFilter) {
        this.includeFilter = includeFilter;
        return this;
      }

      /**
       * The exclude metric filter expression of the reporter.
       *
       * @return the exclude filter
       */
      public String getExcludeFilter() {
        return excludeFilter;
      }

      /**
       * Set the exclude metric filter expression of the reporter.
       *
       * @param excludeFilter the regular expression to match on.
       * @return {@code this}
       */
      public WebSocket excludeFilter(String excludeFilter) {
        this.excludeFilter = excludeFilter;
        return this;
      }
    }

    public static class Csv {
      private Duration reporterInterval = DEFAULT_INTERVAL;
      private File reportDirectory;
      private boolean enabled = true;
      private String includeFilter;
      private String excludeFilter;

      /**
       * The state of the CSV publisher.
       *
       * @return the state of the CSV publisher
       */
      public boolean isEnabled() {
        return enabled;
      }

      /**
       * Set the state of the CSV publisher.
       *
       * @param enabled True if metrics are published to CSV. False otherwise
       * @return this
       */
      public Csv enable(boolean enabled) {
        this.enabled = enabled;
        return this;
      }

      /**
       * The include metric filter expression of the reporter.
       *
       * @return the include filter
       */
      public String getIncludeFilter() {
        return includeFilter;
      }

      /**
       * Set the include metric filter expression of the reporter.
       *
       * @param includeFilter the regular expression to match on.
       * @return {@code this}
       */
      public Csv includeFilter(String includeFilter) {
        this.includeFilter = includeFilter;
        return this;
      }

      /**
       * The exclude metric filter expression of the reporter.
       *
       * @return the exclude filter
       */
      public String getExcludeFilter() {
        return excludeFilter;
      }

      /**
       * Set the exclude metric filter expression of the reporter.
       *
       * @param excludeFilter the regular expression to match on.
       * @return {@code this}
       */
      public Csv excludeFilter(String excludeFilter) {
        this.excludeFilter = excludeFilter;
        return this;
      }

      /**
       * The interval between metrics reports.
       *
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
    bind(Slf4jReporter.class).toProvider(Slf4jReporterProvider.class).in(SINGLETON);
    bind(CsvReporter.class).toProvider(CsvReporterProvider.class).in(SINGLETON);
    bind(MetricRegistryPeriodicPublisher.class).in(SINGLETON);
    bind(MetricsBroadcaster.class).in(SINGLETON);

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
      config.getJmx().ifPresent(jmx -> {
        if (jmx.isEnabled()) {
          injector.getInstance(JmxReporter.class).start();
        }
      });

      config.getConsole().ifPresent(console -> {
        if (console.isEnabled()) {
          injector.getInstance(ConsoleReporter.class).start(console.getReporterInterval().getSeconds(), SECONDS);
        }
      });

      config.getSlf4j().ifPresent(slf4j -> {
        if (slf4j.isEnabled()) {
          injector.getInstance(Slf4jReporter.class).start(slf4j.getReporterInterval().getSeconds(), SECONDS);
        }
      });

      config.getCsv().ifPresent(csv -> {
        if (csv.isEnabled()) {
          injector.getInstance(CsvReporter.class).start(csv.getReporterInterval().getSeconds(), SECONDS);
        }
      });

      if (config.isJvmMetrics()) {
        final MetricRegistry metricRegistry = injector.getInstance(MetricRegistry.class);
        metricRegistry.registerAll(new GarbageCollectorMetricSet());
        metricRegistry.registerAll(new ThreadStatesGaugeSet());
        metricRegistry.registerAll(new MemoryUsageGaugeSet());
      }
    }
  }

  private static class HandlerDecoratorProvider implements Provider<HandlerDecorator> {
    private Config config;

    @Inject
    public HandlerDecoratorProvider(Config config) {
      this.config = config;
    }

    @Override
    public HandlerDecorator get() {
      return HandlerDecorator.prepend(new RequestTimingHandler(config));
    }
  }

}
