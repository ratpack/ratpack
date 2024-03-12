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

package ratpack.dropwizard.metrics;

import com.codahale.metrics.*;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.jmx.JmxReporter;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.dropwizard.samplebuilder.SampleBuilder;
import ratpack.dropwizard.metrics.internal.*;
import ratpack.func.Action;
import ratpack.guice.ConfigurableModule;
import ratpack.core.handling.HandlerDecorator;
import ratpack.core.service.Service;
import ratpack.core.service.StartEvent;
import ratpack.core.service.StopEvent;

import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static com.google.inject.Scopes.SINGLETON;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * An extension module that provides support for Dropwizard Metrics.
 * <p>
 * To use it one has to register the module and enable the required functionality by chaining the various configuration
 * options.  For example, to enable the capturing and reporting of metrics to {@link DropwizardMetricsConfig#jmx(Action)}
 * one would write: (Groovy DSL)
 *
 * <pre class="groovy-ratpack-dsl">
 * import ratpack.dropwizard.metrics.DropwizardMetricsModule
 * import static ratpack.groovy.Groovy.ratpack
 *
 * ratpack {
 *   bindings {
 *     module new DropwizardMetricsModule(), { it.jmx() }
 *   }
 * }
 * </pre>
 *
 * <p>
 * To enable the capturing and reporting of metrics to JMX and the console, one would write: (Groovy DSL)
 *
 * <pre class="groovy-ratpack-dsl">
 * import ratpack.dropwizard.metrics.DropwizardMetricsModule
 * import static ratpack.groovy.Groovy.ratpack
 *
 * ratpack {
 *   bindings {
 *     module new DropwizardMetricsModule(), { it.jmx().console() }
 *   }
 * }
 * </pre>
 *
 * <h2>External Configuration</h2>
 * <p>
 * The module can also be configured via external configuration using the
 * <a href="http://www.ratpack.io/manual/current/api/ratpack/config/ConfigData.html" target="_blank">ratpack-config</a> extension.
 * For example, to enable the capturing and reporting of metrics to jmx via an external property file which can be overridden with
 * system properties one would write: (Groovy DSL)
 *
 * <pre class="groovy-ratpack-dsl">
 * import com.google.common.collect.ImmutableMap
 * import ratpack.dropwizard.metrics.DropwizardMetricsModule
 * import ratpack.dropwizard.metrics.DropwizardMetricsConfig
 * import ratpack.config.ConfigData
 * import static ratpack.groovy.Groovy.ratpack
 *
 * ratpack {
 *   serverConfig {
 *     props(ImmutableMap.of("metrics.jmx.enabled", "true")) // for demo purposes we are using a map to easily see the properties being set
 *     sysProps()
 *     require("/metrics", DropwizardMetricsConfig)
 *   }
 *
 *   bindings {
 *     module DropwizardMetricsModule
 *   }
 * }
 * </pre>
 *
 * <h2>Metric Collection</h2>
 * <p>
 * By default {@link com.codahale.metrics.Timer} metrics are collected for all requests received and {@link Counter} metrics for response codes.
 * The module adds a default {@link RequestTimingHandler} to the handler chain <b>before</b> any user handlers.  This means that response times do not
 * take any framework overhead into account and purely the amount of time spent in handlers.  It is important that the module is registered first
 * in the modules list to ensure that <b>all</b> handlers are included in the metric.
 * <p>
 * The module also adds a default {@link BlockingExecTimingInterceptor} to the execution path. This will add timers that will account for time
 * spent on blocking io calls.
 * <p>
 * Both the request timing handler and the blocking execution timing interceptor can be disabled:
 *
 * <pre class="groovy-ratpack-dsl">{@code
 * import ratpack.dropwizard.metrics.DropwizardMetricsModule
 * import static ratpack.groovy.Groovy.ratpack
 *
 * ratpack {
 *   bindings {
 *     module new DropwizardMetricsModule(), { it.requestTimingMetrics(false).blockingTimingMetrics(false) }
 *   }
 *
 *   handlers {
 *     all {
 *       render ""
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p>
 * Additional custom metrics can be registered with the provided {@link MetricRegistry} instance
 * <p>
 * Example custom metrics: (Groovy DSL)
 *
 * <pre class="groovy-ratpack-dsl">{@code
 * import ratpack.dropwizard.metrics.DropwizardMetricsModule
 * import com.codahale.metrics.MetricRegistry
 * import static ratpack.groovy.Groovy.ratpack
 *
 * ratpack {
 *   bindings {
 *     module new DropwizardMetricsModule(), { it.jmx() }
 *   }
 *
 *   handlers { MetricRegistry metricRegistry ->
 *     all {
 *       metricRegistry.meter("my custom meter").mark()
 *       render ""
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p>
 * Custom metrics can also be added via the Metrics annotations ({@link Metered}, {@link Timed} and {@link com.codahale.metrics.annotation.Gauge})
 * to any Guice injected classes.
 *
 * @see <a href="https://dropwizard.github.io/metrics/3.1.0/manual/" target="_blank">Dropwizard Metrics</a>
 */
public class DropwizardMetricsModule extends ConfigurableModule<DropwizardMetricsConfig> {

  public static final String RATPACK_METRIC_REGISTRY = "ratpack-metrics";

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
    bind(GraphiteReporter.class).toProvider(GraphiteReporterProvider.class).in(SINGLETON);
    bind(MetricRegistryPeriodicPublisher.class).in(SINGLETON);
    bind(MetricsBroadcaster.class).in(SINGLETON);
    bind(HttpClientMetrics.class).in(SINGLETON);
    bind(Startup.class);

    bind(BlockingExecTimingInterceptor.class).toProvider(BlockingExecTimingInterceptorProvider.class).in(SINGLETON);
    bind(RequestTimingHandler.class).toProvider(RequestTimingHandlerProvider.class).in(SINGLETON);
    Provider<RequestTimingHandler> handlerProvider = getProvider(RequestTimingHandler.class);
    Multibinder.newSetBinder(binder(), HandlerDecorator.class).addBinding().toProvider(() -> HandlerDecorator.prepend(handlerProvider.get()));

    bind(CollectorRegistry.class).in(SINGLETON);
  }

  private <T> T injected(T instance) {
    requestInjection(instance);
    return instance;
  }

  private static class Startup implements Service {

    private final DropwizardMetricsConfig config;
    private final Injector injector;

    @Inject
    public Startup(DropwizardMetricsConfig config, Injector injector) {
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

      config.getGraphite().ifPresent(graphite -> {
        if (graphite.isEnabled()) {
          injector.getInstance(GraphiteReporter.class).start(graphite.getReporterInterval().getSeconds(), SECONDS);
        }
      });

      if (config.isJvmMetrics()) {
        final MetricRegistry metricRegistry = injector.getInstance(MetricRegistry.class);
        metricRegistry.registerAll(new GarbageCollectorMetricSet());
        metricRegistry.registerAll(new ThreadStatesGaugeSet());
        metricRegistry.registerAll(new MemoryUsageGaugeSet());
      }

      config.getByteBufAllocator().ifPresent(byteBufAllocatorConfig -> {
        if (byteBufAllocatorConfig.isEnabled()) {
          final MetricRegistry metricRegistry = injector.getInstance(MetricRegistry.class);
          final ByteBufAllocator byteBufAllocator = event.getRegistry().get(ByteBufAllocator.class);

          final MetricSet metricSet;
          if (byteBufAllocator instanceof PooledByteBufAllocator) {
            metricSet = new PooledByteBufAllocatorMetricSet((PooledByteBufAllocator) byteBufAllocator, byteBufAllocatorConfig.isDetailed());
          } else if (byteBufAllocator instanceof UnpooledByteBufAllocator) {
            metricSet = new UnpooledByteBufAllocatorMetricSet((UnpooledByteBufAllocator) byteBufAllocator);
          } else {
            throw new UnsupportedOperationException(String.format("Unknown type of byte buf allocator (%s)", byteBufAllocator.getClass()));
          }

          metricRegistry.registerAll(metricSet);
        }
      });

      if (config.isPrometheusCollection()) {
        final CollectorRegistry collectorRegistry = injector.getInstance(CollectorRegistry.class);
        final MetricRegistry metricRegistry = injector.getInstance(MetricRegistry.class);
        collectorRegistry.register(new DropwizardExports(metricRegistry, new CustomSampleBuilder()));
      }
    }

    @Override
    public void onStop(StopEvent event) throws Exception {
      Iterable<? extends ScheduledReporter> scheduledReporters = event.getRegistry().getAll(ScheduledReporter.class);
      for (ScheduledReporter scheduledReporter : scheduledReporters) {
        if (scheduledReporter != null) {
          scheduledReporter.stop();
        }
      }
    }
  }

  // This is a custom name mapper that replicates that behavior of simply prefixing metrics that start with a digit
  // with an underscore, which changed in https://github.com/prometheus/client_java/pull/435 to replace the
  // leading digit with an underscore instead of prefixing it.
  private static class CustomSampleBuilder implements SampleBuilder {

    private static final Pattern PREFIX = Pattern.compile("^([0-9])");

    @Override
    public Collector.MetricFamilySamples.Sample createSample(final String dropwizardName, final String nameSuffix, final List<String> additionalLabelNames, final List<String> additionalLabelValues, final double value) {
      final String suffix = nameSuffix == null ? "" : nameSuffix;
      final List<String> labelNames = additionalLabelNames == null ? Collections.<String>emptyList() : additionalLabelNames;
      final List<String> labelValues = additionalLabelValues == null ? Collections.<String>emptyList() : additionalLabelValues;
      final String initialName = PREFIX.matcher(dropwizardName).replaceFirst("_$1");
      return new Collector.MetricFamilySamples.Sample(
        Collector.sanitizeMetricName(initialName + suffix),
        new ArrayList<String>(labelNames),
        new ArrayList<String>(labelValues),
        value
      );
    }
  }

}
