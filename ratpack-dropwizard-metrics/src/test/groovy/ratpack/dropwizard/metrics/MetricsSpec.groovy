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

package ratpack.dropwizard.metrics

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.MetricRegistryListener
import com.codahale.metrics.SharedMetricRegistries
import com.codahale.metrics.annotation.Gauge
import com.codahale.metrics.annotation.Metered
import com.codahale.metrics.annotation.Timed
import com.codahale.metrics.graphite.GraphiteSender
import groovy.json.JsonSlurper
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.PooledByteBufAllocator
import io.netty.buffer.UnpooledByteBufAllocator
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.slf4j.Logger
import ratpack.dropwizard.metrics.internal.PooledByteBufAllocatorMetricSet
import ratpack.dropwizard.metrics.internal.UnpooledByteBufAllocatorMetricSet
import ratpack.error.ServerErrorHandler
import ratpack.error.internal.DefaultDevelopmentErrorHandler
import ratpack.exec.Blocking
import ratpack.exec.ExecController
import ratpack.exec.Promise
import ratpack.groovy.handling.GroovyChain
import ratpack.groovy.test.embed.GroovyEmbeddedApp
import ratpack.http.client.HttpClient
import ratpack.test.embed.EmbeddedApp
import ratpack.test.internal.RatpackGroovyDslSpec
import ratpack.websocket.RecordingWebSocketClient
import spock.lang.AutoCleanup
import spock.util.concurrent.BlockingVariable
import spock.util.concurrent.PollingConditions

import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MetricsSpec extends RatpackGroovyDslSpec {

  @SuppressWarnings("GroovyUnusedDeclaration")
  PollingConditions polling = new PollingConditions()

  @Rule
  TemporaryFolder reportDirectory

  @AutoCleanup
  EmbeddedApp otherApp

  EmbeddedApp otherApp(@DelegatesTo(value = GroovyChain, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
    otherApp = GroovyEmbeddedApp.of {
      registryOf { add ServerErrorHandler, new DefaultDevelopmentErrorHandler() }
      handlers(closure)
    }
  }

  URI otherAppUrl(String path = "") {
    new URI("$otherApp.address$path")
  }

  def setup() {
    SharedMetricRegistries.clear()
  }

  def "can register metrics module"() {
    when:
    bindings {
      module new DropwizardMetricsModule(), {}
    }
    handlers { MetricRegistry metrics ->
      all {
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

    def log = Mock(Logger) {
      info(_, _, _) >> { args ->
        println args
      }

      isInfoEnabled(_) >> {
        return  true
      }
    }
    def graphite = Mock(GraphiteSender) {
      send(_,_,_) >> {args -> println(args)}
      isConnected() >> true
      getFailures() >> 0
    }

    and:
    bindings {
      module new DropwizardMetricsModule(), {
        it
          .jmx()
          .csv { it.reportDirectory(reportDirectory.root).reporterInterval(Duration.ofSeconds(1)) }
          .console { it.reporterInterval(Duration.ofSeconds(1)) }
          .slf4j { it.logger(log).reporterInterval(Duration.ofSeconds(1)).prefix("test") }
          .graphite { it.sender(graphite).prefix("graphite").reporterInterval(Duration.ofSeconds(1)) }
      }
    }
    handlers { MetricRegistry metrics ->
      all {
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
      output.toString().contains("root.get-requests") &&
        output.toString().contains("test.root.get-requests") &&
        output.toString().contains("graphite.root.get-requests")
    }

    cleanup:
    System.out = origOut
  }

  def "can collect custom metrics"() {
    def reporter = Mock(MetricRegistryListener)
    def requestMeter

    given:
    bindings {
      module new DropwizardMetricsModule(), {}
    }

    handlers { MetricRegistry metrics ->
      // TODO this is a bad place to do this - We should auto register user added MetricRegistryListeners
      // Also need to consider a more general post startup hook
      metrics.addListener(reporter)

      all {
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

    @Timed(name = 'foo.timer.promise', absolute = true)
    public Promise<String> triggerTimerPromise() { Promise.sync{sleep(50); "resultPromise"} }

    @Timed(name = 'foo.timer.sync', absolute=true)
    public String triggerTimerSync() {sleep(50); "resultSync"}

  }

  def "can collect metered annotated metrics"() {
    def reporter = Mock(MetricRegistryListener)
    def absoluteNamedMeter
    def namedMeter
    def unNamedMeter

    given:
    bindings {
      module new DropwizardMetricsModule(), {}
      bind AnnotatedMetricService
    }

    handlers { MetricRegistry metrics ->
      metrics.addListener(reporter)

      path("meter") { AnnotatedMetricService service ->
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

    1 * reporter.onMeterAdded('ratpack.dropwizard.metrics.MetricsSpec$AnnotatedMetricService.triggerMeter3', !null) >> { arguments ->
      namedMeter = arguments[1]
    }

    1 * reporter.onMeterAdded('ratpack.dropwizard.metrics.MetricsSpec$AnnotatedMetricService.foo meter', !null) >> { arguments ->
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
    bindings {
      module new DropwizardMetricsModule(), {}
      bind AnnotatedMetricService
    }

    handlers { MetricRegistry metrics ->
      metrics.addListener(reporter)

      path("timer") { AnnotatedMetricService service ->
        service
          .triggerTimer1()
          .triggerTimer2()
          .triggerTimer3()
          .triggerTimer1()

        render ""
      }
    }

    when: "Calling the service twice"
    2.times { get("timer") }

    then: "Timers should be registered only once"
    1 * reporter.onTimerAdded("foo timer", !null) >> { arguments ->
      absoluteNamedTimer = arguments[1]
    }

    1 * reporter.onTimerAdded('ratpack.dropwizard.metrics.MetricsSpec$AnnotatedMetricService.triggerTimer3', !null) >> { arguments ->
      namedTimer = arguments[1]
    }

    1 * reporter.onTimerAdded('ratpack.dropwizard.metrics.MetricsSpec$AnnotatedMetricService.foo timer', !null) >> { arguments ->
      unNamedTimer = arguments[1]
    }

    and: "Expect the number timers be named, and un-named"
    absoluteNamedTimer.count == 4
    namedTimer.count == 2
    unNamedTimer.count == 2

  }

  def "can properly capture timing events"() {
    MetricRegistry registry

    given:
    bindings {
      module new DropwizardMetricsModule(), {}
      bind AnnotatedMetricService
    }

    handlers { MetricRegistry metrics ->
      registry = metrics

      path("sync") { AnnotatedMetricService service ->
        render(service.triggerTimerSync())

      }

      path("async") { AnnotatedMetricService service ->
        render(service.triggerTimerPromise())
      }
    }

    when: "Timing synchronous methods"
    def resultSync = get("sync")

    then: "The synchronous methods should take at least the time they slept"
    registry.timers.get('foo.timer.sync').count <= 50
    resultSync.body.text == "resultSync"

    when: "Timing promise methods"
    def resultPromise = get("async")

    then: "The asynchronous methods should take at least the time they slept"
    registry.timers.get('foo.timer.promise').count <= 50
    resultPromise.body.text == "resultPromise"
  }

  def "can collect gauge annotated metrics"() {
    MetricRegistry registry

    given:
    bindings {
      module new DropwizardMetricsModule(), {}
      bind AnnotatedMetricService
    }

    handlers { MetricRegistry metrics ->
      registry = metrics

      path("gauge") { AnnotatedMetricService service ->
        render ""
      }
    }

    when:
    get("gauge")

    then:
    "gauge2" == registry.gauges.get('ratpack.dropwizard.metrics.MetricsSpec$AnnotatedMetricService.foo gauge').value
    "gauge3" == registry.gauges.get('ratpack.dropwizard.metrics.MetricsSpec$AnnotatedMetricService.triggerGauge3').value
    "gauge1" == registry.gauges.get('foo gauge').value
  }

  def "can collect request timer metrics"() {
    def reporter = Mock(MetricRegistryListener)

    given:
    bindings {
      module new DropwizardMetricsModule(), { it.jmx() }
    }

    handlers { MetricRegistry metrics ->

      metrics.addListener(reporter)

      all {
        render ""
      }
      prefix("foo") {
        path("bar") {
          render ""
        }
        all {
          render ""
        }
      }
    }

    when:
    2.times { get() }
    2.times { get("foo") }
    2.times { get("foo/bar") }

    then:
    1 * reporter.onTimerAdded("root.get-requests", !null)
    1 * reporter.onTimerAdded("foo.get-requests", !null)
    1 * reporter.onTimerAdded("foo.bar.get-requests", !null)
  }

  def "can collect jvm metrics"() {
    def reporter = Mock(MetricRegistryListener)

    given:
    bindings {
      module new DropwizardMetricsModule(), { it.jvmMetrics(true) }
    }

    handlers { MetricRegistry metrics ->
      metrics.addListener(reporter)

      all {
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

  def "can collect #allocator metrics"() {
    def reporter = Mock(MetricRegistryListener)

    given:
    bindings {
      bindInstance ByteBufAllocator, allocator
      module new DropwizardMetricsModule(), {
        it.byteBufAllocator { c ->
          c.enable(true)
          c.detail(true)
        }
      }
    }

    handlers { MetricRegistry metrics ->
      metrics.addListener(reporter)

      all {
        render ""
      }
    }

    when:
    get()

    then:
    (1.._) * reporter.onGaugeAdded(!null, {
      it.class.name.startsWith(expected.name)
    })

    where:
    allocator                        | expected
    PooledByteBufAllocator.DEFAULT   | PooledByteBufAllocatorMetricSet
    UnpooledByteBufAllocator.DEFAULT | UnpooledByteBufAllocatorMetricSet
  }

  def "can use metrics endpoint"() {
    given:
    bindings {
      module new DropwizardMetricsModule(), { it.webSocket { it.reporterInterval(Duration.ofMillis(2000)).excludeFilter("2xx-responses") } }
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
    2.times { getText() }

    when:
    def client = openWsClient()
    client.connectBlocking()

    then:
    with(new JsonSlurper().parseText(client.received.poll(2, TimeUnit.SECONDS))) {
      timers.size() == 1
      timers.containsKey("root.get-requests")
      timers["root.get-requests"].count == 2

      gauges.size() == 1
      gauges.containsKey("fooGauge")
      gauges["fooGauge"].value == 2

      meters.size() == 1
      meters.containsKey("fooMeter")
      meters["fooMeter"].count == 2

      counters.size() == 1
      counters.containsKey("fooCounter")
      counters["fooCounter"].count == 2

      histograms.size() == 1
      histograms.containsKey("fooHistogram")
      histograms["fooHistogram"].count == 2
    }

    when:
    2.times { getText() }

    then:
    with(new JsonSlurper().parseText(client.received.poll(2, TimeUnit.SECONDS))) {
      timers.size() == 1
      timers.containsKey("root.get-requests")
      timers["root.get-requests"].count == 4

      gauges.size() == 1
      gauges.containsKey("fooGauge")
      gauges["fooGauge"].value == 2

      meters.size() == 1
      meters.containsKey("fooMeter")
      meters["fooMeter"].count == 4

      counters.size() == 1
      counters.containsKey("fooCounter")
      counters["fooCounter"].count == 4

      histograms.size() == 1
      histograms.containsKey("fooHistogram")
      histograms["fooHistogram"].count == 4
    }

    cleanup:
    client?.closeBlocking()
  }

  def RecordingWebSocketClient openWsClient() {
    new RecordingWebSocketClient(new URI("ws://localhost:$server.bindPort/admin/metrics-report"))
  }

  def "can collect blocking metrics"() {
    def reporter = Mock(MetricRegistryListener)
    def blockingTimer

    given:
    bindings {
      module new DropwizardMetricsModule(), {}
    }

    handlers { MetricRegistry metrics ->
      metrics.addListener(reporter)

      path("foo") {
        Blocking.get {
          2
        } then {
          render ""
        }
      }
    }

    when:
    2.times { get("foo") }

    then:
    1 * reporter.onTimerAdded("foo.get-blocking", !null) >> { arguments ->
      blockingTimer = arguments[1]
    }
    blockingTimer.count == 2
  }

  def "can filter reporters"() {
    given:
    def output = new ByteArrayOutputStream()

    def origOut = System.out
    System.out = new PrintStream(output, true)

    def log = Mock(Logger) {
      info(_, _, _) >> { args ->
        println args
      }

      isInfoEnabled(_) >> {
        return true
      }
    }

    def graphite = Mock(GraphiteSender) {
      send(_,_,_) >> {args -> println(args)}
      isConnected() >> true
      getFailures() >> 0
    }

    and:
    bindings {
      module new DropwizardMetricsModule(), {
        it.console { it.reporterInterval(Duration.ofSeconds(1)).includeFilter(".*ar.*").excludeFilter(".*bar.*") }
        it.slf4j { it.logger(log).reporterInterval(Duration.ofSeconds(1)).prefix("test").includeFilter(".*ar.*").excludeFilter(".*bar.*") }
        it.jmx { it.includeFilter(".*ar.*") }
        it.csv { it.reportDirectory(reportDirectory.root).reporterInterval(Duration.ofSeconds(1)).includeFilter(".*foo.*") }
        it.graphite {it.sender(graphite).prefix("graphite").reporterInterval(Duration.ofSeconds(1)).includeFilter(".*ar.*").excludeFilter(".*bar.*")}
      }
    }

    handlers { MetricRegistry metrics ->
      all { render "" }
    }

    when:
    get("foo")
    get("bar")
    get("tar")

    then:
    polling.within(2) {
      output.toString().contains("tar.get-requests") &&
        output.toString().contains("test.tar.get-requests") &&
        output.toString().contains("graphite.tar.get-requests")
    }

    and:
    !output.toString().contains("foo.get-requests")
    !output.toString().contains("bar.get-requests")
    !output.toString().contains("test.foo.get-requests")
    !output.toString().contains("test.bar.get-requests")
    !output.toString().contains("graphite.foo.get-requests")
    !output.toString().contains("graphite.bar.get-requests")

    and:
    reportDirectory.root.listFiles().length == 1
    reportDirectory.root.listFiles()[0].name.contains("foo")

    cleanup:
    System.out = origOut
  }

  def "can apply custom groups for request timer metrics"() {
    def reporter = Mock(MetricRegistryListener)
    def latch = new CountDownLatch(5)
    given:
    bindings {
      module new DropwizardMetricsModule(), {
        it.requestMetricGroups(["bar": "bar/.*", "foo": "foo/.*", "f": "f.*"])
      }
    }

    handlers { MetricRegistry metrics ->
      metrics.addListener(reporter)
      all {
        onClose { latch.countDown() }
        render ""
      }
    }

    when:
    get("foo/1")
    get("foo/2/tar")
    get("far")
    get("foo/3/bar")
    get("tar?id=3")
    latch.await()

    then:
    1 * reporter.onTimerAdded("tar.get-requests", !null)
    1 * reporter.onTimerAdded("foo.get-requests", !null)
    1 * reporter.onTimerAdded("f.get-requests", !null)
    1 * reporter.onCounterAdded('2xx-responses', !null)
  }

  def "can collect status code metrics"() {
    def reporter = Mock(MetricRegistryListener)
    def twoxxCounter
    def fourxxCounter

    given:
    1 * reporter.onCounterAdded("2xx-responses", !null) >> { arguments ->
      twoxxCounter = arguments[1]
    }
    1 * reporter.onCounterAdded("4xx-responses", !null) >> { arguments ->
      fourxxCounter = arguments[1]
    }

    and:

    bindings {
      module new DropwizardMetricsModule(), {}
    }

    handlers { MetricRegistry metrics ->
      metrics.addListener(reporter)
      path("foo") { render "" }
      path("bar") {
        clientError(401)
      }
    }

    when:
    get("foo")
    get("bar")
    get("tar")

    then:
    new PollingConditions().within(2) {
      twoxxCounter != null
      twoxxCounter.count == 1

      fourxxCounter != null
      fourxxCounter.count == 2
    }
  }

  def "can disable blocking metrics"() {
    def reporter = Mock(MetricRegistryListener)

    given:
    bindings {
      module new DropwizardMetricsModule(), { it.blockingTimingMetrics(false) }
    }

    handlers { MetricRegistry metrics ->
      metrics.addListener(reporter)

      path("foo") {
        Blocking.get {
          2
        } then {
          render ""
        }
      }
    }

    when:
    2.times { get("foo") }

    then:
    1 * reporter.onTimerAdded("foo.get-requests", !null)
    0 * reporter.onTimerAdded("foo.get-blocking", !null)
  }

  def "can disable request timing metrics"() {
    def reporter = Mock(MetricRegistryListener)

    given:
    bindings {
      module new DropwizardMetricsModule(), { it.requestTimingMetrics(false) }
    }

    handlers { MetricRegistry metrics ->
      metrics.addListener(reporter)

      path("foo") {
        Blocking.get {
          2
        } then {
          render ""
        }
      }
    }

    when:
    2.times { get("foo") }

    then:
    1 * reporter.onTimerAdded("foo.get-blocking", !null)
    0 * reporter.onTimerAdded("foo.get-requests", !null)
  }

  def "can use prometheus metrics endpoint"() {
    given:
    bindings {
      module new DropwizardMetricsModule(), { it.prometheusCollection(true) }
    }
    handlers { MetricRegistry metrics ->

      metrics.register("fooGauge", new com.codahale.metrics.Gauge<Integer>() {
        @Override
        Integer getValue() {
          2
        }
      })

      get {
        metrics.meter("fooMeter").mark()
        metrics.counter("fooCounter").inc()
        metrics.histogram("fooHistogram").update(metrics.counter("fooCounter").count)
        render "foo"
      }

      get("admin/metrics-report", new MetricsPrometheusHandler())
    }

    when:
    get("/")
    def metrics = getText("admin/metrics-report").split("\n")

    then:
    metrics.contains("fooGauge 2.0")
    metrics.contains("_2xx_responses 1.0")
    metrics.contains("fooCounter 1.0")
    metrics.contains("fooHistogram{quantile=\"0.5\",} 1.0")
    metrics.contains("fooHistogram{quantile=\"0.75\",} 1.0")
    metrics.contains("fooHistogram{quantile=\"0.95\",} 1.0")
    metrics.contains("fooHistogram{quantile=\"0.98\",} 1.0")
    metrics.contains("fooHistogram{quantile=\"0.99\",} 1.0")
    metrics.contains("fooHistogram{quantile=\"0.999\",} 1.0")
    metrics.contains("fooHistogram_count 1.0")
    metrics.find { it.startsWith("root_get_requests{quantile=\"0.5\",}") }
    metrics.find { it.startsWith("root_get_requests{quantile=\"0.75\",}") }
    metrics.find { it.startsWith("root_get_requests{quantile=\"0.95\",}") }
    metrics.find { it.startsWith("root_get_requests{quantile=\"0.98\",}") }
    metrics.find { it.startsWith("root_get_requests{quantile=\"0.99\",}") }
    metrics.find { it.startsWith("root_get_requests{quantile=\"0.999\",}") }
    metrics.contains("root_get_requests_count 1.0")
    metrics.contains("fooMeter_total 1.0")

  }

  def "it should report http client metrics"() {
    given:
    MetricRegistry registry
    String ok = 'ok'
    def result = new BlockingVariable<String>()
    def httpClient = HttpClient.of { spec ->
      spec.poolSize(0)
      spec.enableMetricsCollection(true)
    }

    bindings {
      bindInstance(HttpClient, httpClient)
      module new DropwizardMetricsModule(), { spec ->
        spec.httpClient { config ->
          config
            .enable(true)
            .pollingFrequencyInSeconds(1)
        }
      }
    }

    otherApp {
      get {
        Blocking.get({
          return result.get()
        })
          .onError(it.&error)
          .then(it.&render)
      }
    }

    handlers { MetricRegistry metrics ->
      registry = metrics
      get {
        ExecController execController = it.get(ExecController)
        execController.fork().start({
          httpClient.get(otherAppUrl())
            .then({ val ->
            assert val.body.text == ok
          })
        })
        render ok
      }
    }

    when:
    text == ok

    then:
    polling.within(2) {
      assert registry.getGauges().get('httpclient.total.active.connections').value == 1
      assert registry.getGauges().get('httpclient.total.idle.connections').value == 0
      assert registry.getGauges().get('httpclient.total.connections').value == 1
      assert registry.getGauges().get("httpclient.${otherAppUrl().host}.total.active.connections").value == 1
      assert registry.getGauges().get("httpclient.${otherAppUrl().host}.total.idle.connections").value == 0
      assert registry.getGauges().get("httpclient.${otherAppUrl().host}.total.connections").value == 1


    }

    when:
    result.set(ok)

    then:
    polling.within(2) {
      assert registry.getGauges().get('httpclient.total.active.connections').value == 0
      assert registry.getGauges().get('httpclient.total.idle.connections').value == 0
      assert registry.getGauges().get('httpclient.total.connections').value == 0
      assert registry.getGauges().get("httpclient.${otherAppUrl().host}.total.active.connections").value == 0
      assert registry.getGauges().get("httpclient.${otherAppUrl().host}.total.idle.connections").value == 0
      assert registry.getGauges().get("httpclient.${otherAppUrl().host}.total.connections").value == 0
    }
  }
}
