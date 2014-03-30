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

package ratpack.codahale.metrics

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.MetricRegistryListener
import com.codahale.metrics.annotation.Gauge
import com.codahale.metrics.annotation.Metered
import com.codahale.metrics.annotation.Timed
import groovy.json.JsonSlurper
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.websocket.RecordingWebSocketClient
import spock.util.concurrent.PollingConditions

import java.util.concurrent.TimeUnit

class MetricsSpec extends RatpackGroovyDslSpec {

  @SuppressWarnings("GroovyUnusedDeclaration")
  PollingConditions polling = new PollingConditions()

  @Rule
  TemporaryFolder reportDirectory

  def "can register metrics module"() {
    when:
    modules {
      register new CodaHaleMetricsModule().metrics()
    }
    handlers { MetricRegistry metrics ->
      handler {
        render metrics.getClass().name
      }
    }

    then:
    text == "com.codahale.metrics.MetricRegistry"
  }

  def "can register reporters"() {
    given:
    List<MetricRegistryListener> listeners = null
    def output = new ByteArrayOutputStream()

    def origOut = System.out
    System.out = new PrintStream(output, true)

    and:
    launchConfig {
      other("metrics.scheduledreporter.interval", "1")
    }

    and:
    modules {
      register new CodaHaleMetricsModule().jmx().csv(reportDirectory.root).console()
    }
    handlers { MetricRegistry metrics ->
      handler {
        //noinspection GroovyAccessibility
        listeners = metrics.listeners
        render "ok"
      }
    }

    when:
    text

    then:
    listeners.size() == 1 // JMX listener
    polling.within(2) {
      reportDirectory.root.listFiles().length > 0
    }
    polling.within(2) {
      output.toString().contains("[root]~GET~Request")
    }

    cleanup:
    System.out = origOut
  }

  def "can collect custom metrics"() {
    def reporter = Mock(MetricRegistryListener)
    def requestMeter

    given:
    modules {
      register new CodaHaleMetricsModule().metrics()
    }

    handlers { MetricRegistry metrics ->
      // TODO this is a bad place to do this - We should auto register user added MetricRegistryListeners
      // Also need to consider a more general post startup hook
      metrics.addListener(reporter)

      handler {
        metrics.meter("requests").mark()
        render ""
      }
    }

    when:
    2.times { get() }

    then:
    1 * reporter.onMeterAdded("requests", !null) >> { arguments ->
      requestMeter = arguments[1]
    }
    requestMeter.count == 2
  }

  @com.google.inject.Singleton
  static class AnnotatedMetricService {

    @Metered(name = 'foo meter', absolute = true)
    public AnnotatedMetricService triggerMeter1() { this }

    @Metered(name = 'foo meter')
    public AnnotatedMetricService triggerMeter2() { this }

    @Metered
    public AnnotatedMetricService triggerMeter3() { this }

    @Gauge(name = 'foo gauge', absolute = true)
    public String triggerGauge1() { "gauge1" }

    @Gauge(name = 'foo gauge')
    public String triggerGauge2() { "gauge2" }

    @Gauge
    public String triggerGauge3() { "gauge3" }

    @Timed(name = 'foo timer', absolute = true)
    public AnnotatedMetricService triggerTimer1() { this }

    @Timed(name = 'foo timer')
    public AnnotatedMetricService triggerTimer2() { this }

    @Timed
    public AnnotatedMetricService triggerTimer3() { this }

  }

  def "can collect metered annotated metrics"() {
    def reporter = Mock(MetricRegistryListener)
    def absoluteNamedMeter
    def namedMeter
    def unNamedMeter

    given:
    modules {
      register new CodaHaleMetricsModule().metrics()
      bind AnnotatedMetricService
    }

    handlers { MetricRegistry metrics ->
      metrics.addListener(reporter)

      handler("meter") { AnnotatedMetricService service ->
        service
          .triggerMeter1()
          .triggerMeter2()
          .triggerMeter3()
          .triggerMeter1()

        render ""
      }
    }

    when:
    2.times { get("meter") }

    then:
    1 * reporter.onMeterAdded("foo meter", !null) >> { arguments ->
      absoluteNamedMeter = arguments[1]
    }

    1 * reporter.onMeterAdded('ratpack.codahale.metrics.MetricsSpec$AnnotatedMetricService.triggerMeter3', !null) >> { arguments ->
      namedMeter = arguments[1]
    }

    1 * reporter.onMeterAdded('ratpack.codahale.metrics.MetricsSpec$AnnotatedMetricService.foo meter', !null) >> { arguments ->
      unNamedMeter = arguments[1]
    }

    absoluteNamedMeter.count == 4
    namedMeter.count == 2
    unNamedMeter.count == 2
  }

  def "can collect timed annotated metrics"() {
    def reporter = Mock(MetricRegistryListener)
    def absoluteNamedTimer
    def namedTimer
    def unNamedTimer

    given:
    modules {
      register new CodaHaleMetricsModule().metrics()
      bind AnnotatedMetricService
    }

    handlers { MetricRegistry metrics ->
      metrics.addListener(reporter)

      handler("timer") { AnnotatedMetricService service ->
        service
          .triggerTimer1()
          .triggerTimer2()
          .triggerTimer3()
          .triggerTimer1()

        render ""
      }
    }

    when:
    2.times { get("timer") }

    then:
    1 * reporter.onTimerAdded("foo timer", !null) >> { arguments ->
      absoluteNamedTimer = arguments[1]
    }

    1 * reporter.onTimerAdded('ratpack.codahale.metrics.MetricsSpec$AnnotatedMetricService.triggerTimer3', !null) >> { arguments ->
      namedTimer = arguments[1]
    }

    1 * reporter.onTimerAdded('ratpack.codahale.metrics.MetricsSpec$AnnotatedMetricService.foo timer', !null) >> { arguments ->
      unNamedTimer = arguments[1]
    }

    absoluteNamedTimer.count == 4
    namedTimer.count == 2
    unNamedTimer.count == 2
  }

  def "can collect gauge annotated metrics"() {
    MetricRegistry registry

    given:
    modules {
      register new CodaHaleMetricsModule().metrics()
      bind AnnotatedMetricService
    }

    handlers { MetricRegistry metrics ->
      registry = metrics

      handler("gauge") { AnnotatedMetricService service ->
        render ""
      }
    }

    when:
    get("gauge")

    then:
    "gauge2" == registry.gauges.get('ratpack.codahale.metrics.MetricsSpec$AnnotatedMetricService.foo gauge').value
    "gauge3" == registry.gauges.get('ratpack.codahale.metrics.MetricsSpec$AnnotatedMetricService.triggerGauge3').value
    "gauge1" == registry.gauges.get('foo gauge').value
  }

  def "can collect request timer metrics"() {
    def reporter = Mock(MetricRegistryListener)

    given:
    modules {
      register new CodaHaleMetricsModule().jmx()
    }

    handlers { MetricRegistry metrics ->

      metrics.addListener(reporter)

      handler {
        render ""
      }
      prefix("foo") {
        handler("bar") {
          render ""
        }
        handler {
          render ""
        }
      }
    }

    when:
    2.times { get() }
    2.times { get("foo") }
    2.times { get("foo/bar") }

    then:
    1 * reporter.onTimerAdded("[root]~GET~Request", !null)
    1 * reporter.onTimerAdded("[foo]~GET~Request", !null)
    1 * reporter.onTimerAdded("[foo][bar]~GET~Request", !null)
  }

  def "can collect jvm metrics"() {
    def reporter = Mock(MetricRegistryListener)

    given:
    modules {
      register new CodaHaleMetricsModule().jvmMetrics()
    }

    handlers { MetricRegistry metrics ->
      metrics.addListener(reporter)

      handler {
        render ""
      }
    }

    when:
    get()

    then:
    (1.._) * reporter.onGaugeAdded(!null, { it.class.name.startsWith("com.codahale.metrics.jvm.GarbageCollectorMetricSet") })
    (1.._) * reporter.onGaugeAdded(!null, { it.class.name.startsWith("com.codahale.metrics.jvm.ThreadStatesGaugeSet") })
    (1.._) * reporter.onGaugeAdded(!null, { it.class.name.startsWith("com.codahale.metrics.jvm.MemoryUsageGaugeSet") })
  }

  def "can use metrics endpoint"() {
    given:
    launchConfig {
      other("metrics.scheduledreporter.interval", "1")
    }

    and:
    modules {
      register new CodaHaleMetricsModule().websocket()
    }
    handlers { MetricRegistry metrics ->

      metrics.register("fooGauge", new com.codahale.metrics.Gauge<Integer>() {
        @Override
        public Integer getValue() {
          2
        }
      })

      get {
        metrics.meter("fooMeter").mark()
        metrics.counter("fooCounter").inc()
        metrics.histogram("fooHistogram").update(metrics.counter("fooCounter").count)
        render "foo"
      }

      get("admin/metrics-report", new MetricsWebsocketBroadcastHandler())
    }

    and:
    server.start()
    2.times { getText() }

    when:
    def client = openWsClient()
    client.connectBlocking()

    then:
    def response = new JsonSlurper().parseText(client.received.poll(2, TimeUnit.SECONDS))
    response.timers.size() == 2
    response.timers[0].name == "[admin][metrics-report]~GET~Request"
    response.timers[0].count == 0
    response.timers[1].name == "[root]~GET~Request"
    response.timers[1].count == 2

    response.gauges.size() == 1
    response.gauges[0].name == "fooGauge"
    response.gauges[0].value == 2

    response.meters.size() == 1
    response.meters[0].name == "fooMeter"
    response.meters[0].count == 2

    response.counters.size() == 1
    response.counters[0].name == "fooCounter"
    response.counters[0].count == 2

    response.histograms.size() == 1
    response.histograms[0].name == "fooHistogram"
    response.histograms[0].count == 2

    cleanup:
    client?.closeBlocking()
  }

  def RecordingWebSocketClient openWsClient() {
    new RecordingWebSocketClient(new URI("ws://localhost:$server.bindPort/admin/metrics-report"))
  }

  def "can collect background metrics"() {
    def reporter = Mock(MetricRegistryListener)
    def backgroundTimer

    given:
    modules {
      register new CodaHaleMetricsModule().metrics()
    }

    handlers {MetricRegistry metrics ->
      metrics.addListener(reporter)

      handler("foo") {
        background {
          2
        } then {
          render ""
        }
      }
    }

    when:
    2.times { get("foo") }

    then:
    1 * reporter.onTimerAdded("[foo]~GET~Background", !null) >> { arguments ->
      backgroundTimer = arguments[1]
    }
    backgroundTimer.count == 2
  }
}
