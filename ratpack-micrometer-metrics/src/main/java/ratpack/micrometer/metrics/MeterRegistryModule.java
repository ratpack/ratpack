/*
 * Copyright 2020 the original author or authors.
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

package ratpack.micrometer.metrics;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MultibindingsScanner;
import com.google.inject.multibindings.ProvidesIntoSet;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import ratpack.api.Nullable;
import ratpack.micrometer.metrics.config.MappedMeterRegistryConfig;
import ratpack.micrometer.metrics.config.RatpackMeterRegistryConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.stream.Collectors.toList;

public class MeterRegistryModule extends AbstractModule {
  @Override
  protected void configure() {
    install(MultibindingsScanner.asModule());
  }

  @Provides
  @Singleton
  public MeterRegistry meterRegistry(MicrometerMetricsConfig config, Set<MeterRegistryAntiHighlander> registryProviders) {
    List<MeterRegistry> registries = registryProviders.stream()
      .map(MeterRegistryAntiHighlander::getRegistry)
      .filter(Objects::nonNull)
      .collect(toList());

    registries.addAll(config.getAdditionalMeterRegistries());

    MeterRegistry meterRegistry = registries.size() == 1 ? registries.get(0) : new CompositeMeterRegistry(config.getClock(), registries);

    if(config.isUseGlobalRegistry()) {
      Metrics.globalRegistry.add(meterRegistry);
    }

    List<MeterBinder> meterBinders = new ArrayList<>(config.getMeterBinders());
    meterBinders.add(new ProcessorMetrics());
    meterBinders.add(new JvmGcMetrics());
    meterBinders.add(new JvmMemoryMetrics());
    meterBinders.add(new JvmThreadMetrics());
    meterBinders.add(new FileDescriptorMetrics());
    meterBinders.add(new ClassLoaderMetrics());
    meterBinders.add(new UptimeMetrics());

    // Filters must be before binders, so that if there are any tag/name mappings happening in filters, they
    // are effective in the binders
    config.getMeterFilters().forEach(mf -> meterRegistry.config().meterFilter(mf));
    meterBinders.forEach(mb -> mb.bindTo(meterRegistry));

    return meterRegistry;
  }

  @ProvidesIntoSet
  public MeterRegistryAntiHighlander appOpticsMeterRegistry(MicrometerMetricsConfig config) {
    return provideMeterRegistry(config.getAppOptics(),
      "ratpack.micrometer.metrics.config.MappedAppOpticsConfig", config.getClock());
  }

  @ProvidesIntoSet
  public MeterRegistryAntiHighlander atlasMeterRegistry(MicrometerMetricsConfig config) {
    return provideMeterRegistry(config.getAtlas(),
      "ratpack.micrometer.metrics.config.MappedAtlasConfig", config.getClock());
  }

  @ProvidesIntoSet
  public MeterRegistryAntiHighlander datadogMeterRegistry(MicrometerMetricsConfig config) {
    return provideMeterRegistry(config.getDatadog(),
      "ratpack.micrometer.metrics.config.MappedDatadogConfig", config.getClock());
  }

  @ProvidesIntoSet
  public MeterRegistryAntiHighlander dynatraceMeterRegistry(MicrometerMetricsConfig config) {
    return provideMeterRegistry(config.getDynatrace(),
      "ratpack.micrometer.metrics.config.MappedDynatraceConfig", config.getClock());
  }

  @ProvidesIntoSet
  public MeterRegistryAntiHighlander elasticMeterRegistry(MicrometerMetricsConfig config) {
    return provideMeterRegistry(config.getElastic(),
      "ratpack.micrometer.metrics.config.MappedElasticConfig", config.getClock());
  }

  @ProvidesIntoSet
  public MeterRegistryAntiHighlander humioMeterRegistry(MicrometerMetricsConfig config) {
    return provideMeterRegistry(config.getHumio(),
      "ratpack.micrometer.metrics.config.MappedHumioConfig", config.getClock());
  }

  @ProvidesIntoSet
  public MeterRegistryAntiHighlander influxMeterRegistry(MicrometerMetricsConfig config) {
    return provideMeterRegistry(config.getInflux(),
      "ratpack.micrometer.metrics.config.MappedInfluxConfig", config.getClock());
  }

  @ProvidesIntoSet
  public MeterRegistryAntiHighlander KairosMeterRegistry(MicrometerMetricsConfig config) {
    return provideMeterRegistry(config.getKairos(),
      "ratpack.micrometer.metrics.config.MappedKairosConfig", config.getClock());
  }

  @ProvidesIntoSet
  public MeterRegistryAntiHighlander newRelicMeterRegistry(MicrometerMetricsConfig config) {
    return provideMeterRegistry(config.getNewRelic(),
      "ratpack.micrometer.metrics.config.MappedNewRelicConfig", config.getClock());
  }

  @ProvidesIntoSet
  public MeterRegistryAntiHighlander signalFxMeterRegistry(MicrometerMetricsConfig config) {
    return provideMeterRegistry(config.getSignalFx(),
      "ratpack.micrometer.metrics.config.MappedSignalFxConfig", config.getClock());
  }

  @ProvidesIntoSet
  public MeterRegistryAntiHighlander statsdMeterRegistry(MicrometerMetricsConfig config) {
    return provideMeterRegistry(config.getStatsd(),
      "ratpack.micrometer.metrics.config.MappedStatsdConfig", config.getClock());
  }

  @ProvidesIntoSet
  public MeterRegistryAntiHighlander wavefrontMeterRegistry(MicrometerMetricsConfig config) {
    return provideMeterRegistry(config.getWavefront(),
      "ratpack.micrometer.metrics.config.MappedWavefrontConfig", config.getClock());
  }

  @ProvidesIntoSet
  public MeterRegistryAntiHighlander prometheusMeterRegistry(MicrometerMetricsConfig config) {
    return provideMeterRegistry(config.getPrometheus(),
      "ratpack.micrometer.metrics.config.MappedPrometheusConfig", config.getClock());
  }

  /**
   * @param config The ratpack configuration object
   * @param mappedConfigClassName This must be created by class name, since it refers to a dependency
   *                              which may not be on the classpath
   * @param clock The clock to use for timings
   * @return A {@link MeterRegistry} wrapped for indirection so that there is one registry on the Guice injector.
   */
  private MeterRegistryAntiHighlander provideMeterRegistry(RatpackMeterRegistryConfig config,
                                                           String mappedConfigClassName,
                                                           Clock clock) {
    try {
      if(config.isEnabled()) {
        Object mappedConfig = Class.forName(mappedConfigClassName).getDeclaredConstructor(config.getClass()).newInstance(config);
        return new MeterRegistryAntiHighlander(((MappedMeterRegistryConfig) mappedConfig).buildRegistry(clock));
      }
    } catch (Throwable ignored) {
    }
    return new MeterRegistryAntiHighlander(null);
  }

  /**
   * So that there is only one injectable {@link MeterRegistry} in the Guice injector in the end.
   * "There can only be one....err MANY".
   */
  private static class MeterRegistryAntiHighlander {
    @Nullable
    private final MeterRegistry registry;

    MeterRegistryAntiHighlander(MeterRegistry registry) {
      this.registry = registry;
    }

    MeterRegistry getRegistry() {
      return registry;
    }
  }
}
