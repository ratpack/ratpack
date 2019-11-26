package ratpack.micrometer.metrics;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.config.MeterFilter;
import ratpack.func.Action;
import ratpack.handling.Context;
import ratpack.micrometer.metrics.config.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static ratpack.util.Exceptions.uncheck;

public class MicrometerMetricsConfig {
  private Clock clock = Clock.SYSTEM;

  /**
   * Whether auto-configured MeterRegistry implementations should be bound to the global
   * static registry on Metrics. For testing, set this to 'false' to maximize test
   * independence.
   */
  private boolean useGlobalRegistry = true;

  /**
   * Tags that will appear on every metric (useful for tags like for application name, deployed environment,
   * region, etc.).
   */
  private Tags commonTags = Tags.empty();

  private boolean requestTimingMetrics = true;
  private boolean blockingTimingMetrics = true;

  /**
   * To provide {@link MeterRegistry} instances that will not be automatically configured by Ratpack.
   */
  private List<MeterRegistry> additionalMeterRegistries = new ArrayList<>();

  private List<MeterFilter> meterFilters = new ArrayList<>();
  private List<MeterBinder> meterBinders = new ArrayList<>();

  /**
   * Tags that will be added to {@link ratpack.handling.Handler} timings based on the original request and
   * the response.
   */
  private BiFunction<Context, Throwable, Tags> handlerTags = HandlerTags.RECOMMENDED_TAGS;

  private RatpackAppOpticsConfig appOptics = new RatpackAppOpticsConfig();
  private RatpackAtlasConfig atlas = new RatpackAtlasConfig();
  private RatpackDatadogConfig datadog = new RatpackDatadogConfig();
  private RatpackDynatraceConfig dynatrace = new RatpackDynatraceConfig();
  private RatpackElasticConfig elastic = new RatpackElasticConfig();
  private RatpackHumioConfig humio = new RatpackHumioConfig();
  private RatpackInfluxConfig influx = new RatpackInfluxConfig();
  private RatpackKairosConfig kairos = new RatpackKairosConfig();
  private RatpackNewRelicConfig newRelic = new RatpackNewRelicConfig();
  private RatpackPrometheusConfig prometheus = new RatpackPrometheusConfig();
  private RatpackSignalFxConfig signalFx = new RatpackSignalFxConfig();
  private RatpackStatsdConfig statsd = new RatpackStatsdConfig();
  private RatpackWavefrontConfig wavefront = new RatpackWavefrontConfig();

  public Clock getClock() {
    return clock;
  }

  public void setClock(Clock clock) {
    this.clock = clock;
  }

  public boolean isUseGlobalRegistry() {
    return useGlobalRegistry;
  }

  public MicrometerMetricsConfig useGlobalRegistry(boolean useGlobalRegistry) {
    this.useGlobalRegistry = useGlobalRegistry;
    return this;
  }

  public boolean isRequestTimingMetrics() {
    return requestTimingMetrics;
  }

  public MicrometerMetricsConfig requestTimingMetrics(boolean requestTimingMetrics) {
    this.requestTimingMetrics = requestTimingMetrics;
    return this;
  }

  public boolean isBlockingTimingMetrics() {
    return blockingTimingMetrics;
  }

  public MicrometerMetricsConfig blockingTimingMetrics(boolean blockingTimingMetrics) {
    this.blockingTimingMetrics = blockingTimingMetrics;
    return this;
  }

  public List<MeterRegistry> getAdditionalMeterRegistries() {
    return additionalMeterRegistries;
  }

  public MicrometerMetricsConfig additionalMeterRegistries(MeterRegistry... additionalMeterRegistries) {
    Collections.addAll(this.additionalMeterRegistries, additionalMeterRegistries);
    return this;
  }

  public Tags getCommonTags() {
    return commonTags;
  }

  public MicrometerMetricsConfig commonTags(Tags tags) {
    this.commonTags = tags;
    return this;
  }

  public MicrometerMetricsConfig commonTags(Tag... tags) {
    this.commonTags = Tags.of(tags);
    return this;
  }

  public MicrometerMetricsConfig commonTags(Map<String, String> commonTags) {
    this.commonTags = Tags.of(commonTags.entrySet().stream()
      .map(tag -> Tag.of(tag.getKey(), tag.getValue()))
      .collect(Collectors.toList()));
    return this;
  }

  public BiFunction<Context, Throwable, Tags> getHandlerTags() {
    return handlerTags;
  }

  /**
   * Configure tags that will be included in {@link ratpack.handling.Handler} timings based on the original
   * request and the response.
   * @param handlerTags NOTE: You can extend a carefully curated set of tags from {@link HandlerTags#RECOMMENDED_TAGS}.
   * @see #addHandlerTags(BiFunction) to add tags to the recommended list.
   */
  public MicrometerMetricsConfig handlerTags(BiFunction<Context, Throwable, Tags> handlerTags) {
    this.handlerTags = handlerTags;
    return this;
  }

  /**
   * Add additional tags that will be included in {@link ratpack.handling.Handler} timings based on the original
   * request and the response.
   */
  public MicrometerMetricsConfig addHandlerTags(BiFunction<Context, Throwable, Tags> additionalTags) {
    BiFunction<Context, Throwable, Tags> current = this.handlerTags;
    this.handlerTags = (context, throwable) -> Tags.concat(
      current.apply(context, throwable),
      additionalTags.apply(context, throwable)
    );
    return this;
  }

  public List<MeterFilter> getMeterFilters() {
    return meterFilters;
  }

  public MicrometerMetricsConfig meterFilters(List<MeterFilter> meterFilters) {
    this.meterFilters = meterFilters;
    return this;
  }

  public MicrometerMetricsConfig addMeterFilters(MeterFilter... meterFilters) {
    Collections.addAll(this.meterFilters, meterFilters);
    return this;
  }

  public List<MeterBinder> getMeterBinders() {
    return meterBinders;
  }

  public MicrometerMetricsConfig meterBinders(List<MeterBinder> meterBinders) {
    this.meterBinders = meterBinders;
    return this;
  }

  public MicrometerMetricsConfig addMeterBinders(MeterBinder... meterBinders) {
    Collections.addAll(this.meterBinders, meterBinders);
    return this;
  }

  public RatpackAppOpticsConfig getAppOptics() {
    return appOptics;
  }

  /**
   * @see #appOptics(ratpack.func.Action)
   * @return this
   */
  public MicrometerMetricsConfig appOptics() {
    return appOptics(Action.noop());
  }

  /**
   * Configure the AppOptics metrics publisher.
   *
   * @param configure the configuration for the publisher
   * @return this
   */
  public MicrometerMetricsConfig appOptics(Action<? super RatpackAppOpticsConfig> configure) {
    return configAction(appOptics, configure);
  }

  public RatpackAtlasConfig getAtlas() {
    return atlas;
  }

  /**
   * @see #atlas(ratpack.func.Action)
   * @return this
   */
  public MicrometerMetricsConfig atlas() {
    return atlas(Action.noop());
  }

  /**
   * Configure the Atlas metrics publisher.
   *
   * @param configure the configuration for the publisher
   * @return this
   */
  public MicrometerMetricsConfig atlas(Action<? super RatpackAtlasConfig> configure) {
    return configAction(atlas, configure);
  }

  public RatpackDatadogConfig getDatadog() {
    return datadog;
  }

  /**
   * @see #datadog(ratpack.func.Action)
   * @return this
   */
  public MicrometerMetricsConfig datadog() {
    return datadog(Action.noop());
  }

  /**
   * Configure the Datadog metrics publisher.
   *
   * @param configure the configuration for the publisher
   * @return this
   */
  public MicrometerMetricsConfig datadog(Action<? super RatpackDatadogConfig> configure) {
    return configAction(datadog, configure);
  }

  public RatpackDynatraceConfig getDynatrace() {
    return dynatrace;
  }

  /**
   * @see #dynatrace(ratpack.func.Action)
   * @return this
   */
  public MicrometerMetricsConfig dynatrace() {
    return dynatrace(Action.noop());
  }

  /**
   * Configure the Dynatrace metrics publisher.
   *
   * @param configure the configuration for the publisher
   * @return this
   */
  public MicrometerMetricsConfig dynatrace(Action<? super RatpackDynatraceConfig> configure) {
    return configAction(dynatrace, configure);
  }

  public RatpackElasticConfig getElastic() {
    return elastic;
  }

  /**
   * @see #elastic(ratpack.func.Action)
   * @return this
   */
  public MicrometerMetricsConfig elastic() {
    return elastic(Action.noop());
  }

  /**
   * Configure the Elastic metrics publisher.
   *
   * @param configure the configuration for the publisher
   * @return this
   */
  public MicrometerMetricsConfig elastic(Action<? super RatpackElasticConfig> configure) {
    return configAction(elastic, configure);
  }

  public RatpackHumioConfig getHumio() {
    return humio;
  }

  /**
   * @see #appOptics(ratpack.func.Action)
   * @return this
   */
  public MicrometerMetricsConfig humio() {
    return humio(Action.noop());
  }

  /**
   * Configure the Humio metrics publisher.
   *
   * @param configure the configuration for the publisher
   * @return this
   */
  public MicrometerMetricsConfig humio(Action<? super RatpackHumioConfig> configure) {
    return configAction(humio, configure);
  }

  public RatpackInfluxConfig getInflux() {
    return influx;
  }

  /**
   * @see #influx(ratpack.func.Action)
   * @return this
   */
  public MicrometerMetricsConfig influx() {
    return influx(Action.noop());
  }

  /**
   * Configure the Influx metrics publisher.
   *
   * @param configure the configuration for the publisher
   * @return this
   */
  public MicrometerMetricsConfig influx(Action<? super RatpackInfluxConfig> configure) {
    return configAction(influx, configure);
  }

  public RatpackKairosConfig getKairos() {
    return kairos;
  }

  /**
   * @see #kairos(ratpack.func.Action)
   * @return this
   */
  public MicrometerMetricsConfig kairos() {
    return appOptics(Action.noop());
  }

  /**
   * Configure the Kairos metrics publisher.
   *
   * @param configure the configuration for the publisher
   * @return this
   */
  public MicrometerMetricsConfig kairos(Action<? super RatpackKairosConfig> configure) {
    return configAction(kairos, configure);
  }

  public RatpackNewRelicConfig getNewRelic() {
    return newRelic;
  }

  /**
   * @see #newRelic(ratpack.func.Action)
   * @return this
   */
  public MicrometerMetricsConfig newRelic() {
    return newRelic(Action.noop());
  }

  /**
   * Configure the New Relic metrics publisher.
   *
   * @param configure the configuration for the publisher
   * @return this
   */
  public MicrometerMetricsConfig newRelic(Action<? super RatpackNewRelicConfig> configure) {
    return configAction(newRelic, configure);
  }

  public RatpackPrometheusConfig getPrometheus() {
    return prometheus;
  }

  /**
   * @see #prometheus(ratpack.func.Action)
   * @return this
   */
  public MicrometerMetricsConfig prometheus() {
    return prometheus(Action.noop());
  }

  /**
   * Configure the Prometheus metrics publisher.
   *
   * @param configure the configuration for the publisher
   * @return this
   */
  public MicrometerMetricsConfig prometheus(Action<? super RatpackPrometheusConfig> configure) {
    return configAction(prometheus, configure);
  }

  public RatpackSignalFxConfig getSignalFx() {
    return signalFx;
  }

  /**
   * @see #signalFx(ratpack.func.Action)
   * @return this
   */
  public MicrometerMetricsConfig signalFx() {
    return signalFx(Action.noop());
  }

  /**
   * Configure the StatsD metrics publisher.
   *
   * @param configure the configuration for the publisher
   * @return this
   */
  public MicrometerMetricsConfig statsd(Action<? super RatpackStatsdConfig> configure) {
    return configAction(statsd, configure);
  }

  public RatpackStatsdConfig getStatsd() {
    return statsd;
  }

  /**
   * @see #signalFx(ratpack.func.Action)
   * @return this
   */
  public MicrometerMetricsConfig statsd() {
    return statsd(Action.noop());
  }

  /**
   * Configure the SignalFx metrics publisher.
   *
   * @param configure the configuration for the publisher
   * @return this
   */
  public MicrometerMetricsConfig signalFx(Action<? super RatpackSignalFxConfig> configure) {
    return configAction(signalFx, configure);
  }

  public RatpackWavefrontConfig getWavefront() {
    return wavefront;
  }

  /**
   * @see #wavefront(ratpack.func.Action)
   * @return this
   */
  public MicrometerMetricsConfig wavefront() {
    return wavefront(Action.noop());
  }

  /**
   * Configure the Wavefront metrics publisher.
   *
   * @param configure the configuration for the publisher
   * @return this
   */
  public MicrometerMetricsConfig wavefront(Action<? super RatpackWavefrontConfig> configure) {
    return configAction(wavefront, configure);
  }

  private <T> MicrometerMetricsConfig configAction(T config, Action<? super T> configure) {
    try {
      configure.execute(config);
    } catch (Exception e) {
      throw uncheck(e);
    }
    return this;
  }
}
