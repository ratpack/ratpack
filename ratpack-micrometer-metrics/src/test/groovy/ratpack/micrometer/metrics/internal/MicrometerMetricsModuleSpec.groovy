package ratpack.micrometer.metrics.internal

import com.codahale.metrics.annotation.Gauge
import com.codahale.metrics.annotation.Metered
import com.codahale.metrics.annotation.Timed
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import ratpack.exec.Blocking
import ratpack.exec.Promise
import ratpack.micrometer.metrics.MicrometerMetricsModule
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.Shared

import java.util.concurrent.TimeUnit

class MicrometerMetricsModuleSpec extends RatpackGroovyDslSpec {
  @Shared
  Tags successfulGetTags = Tags.of('method', 'GET', 'status', '200', 'outcome', 'SUCCESS')

  def 'custom timing of handler'() {
    given:
    MeterRegistry meterRegistry = null

    bindings {
      module new MicrometerMetricsModule(), {}
    }

    handlers { MeterRegistry registry ->
      meterRegistry = registry
      all {
        registry.timer('render').record {
          render ''
        }
      }
    }

    when:
    2.times { get() }

    then:
    meterRegistry?.find('render')?.timer()
  }

  @com.google.inject.Singleton
  static class AnnotatedMetricService {
    @Metered(name = 'foo.meter')
    AnnotatedMetricService triggerMeter1() { this }

    @Metered
    AnnotatedMetricService triggerMeter2() { this }

    @Gauge(name = 'foo.gauge')
    Integer triggerGauge1() { 1 }

    @Gauge
    String triggerGauge2() { 'gauge' }


    @Timed(name = 'dropwizard.timed')
    AnnotatedMetricService triggerTimer1() { this }

    @Timed
    AnnotatedMetricService triggerTimer2() { this }

    @io.micrometer.core.annotation.Timed('micrometer.timed')
    AnnotatedMetricService triggerTimer3() { this }

    @Timed(name = 'foo.timer.promise')
    Promise<String> triggerTimerPromise() { Promise.sync{sleep(50); 'resultPromise'} }

    @Timed(name = 'foo.timer.sync')
    String triggerTimerSync() {sleep(50); 'resultSync'}
  }

  def 'can collect metered/timed annotated metrics'() {
    given:
    MeterRegistry meterRegistry = null

    bindings {
      module new MicrometerMetricsModule(), {
        it.additionalMeterRegistries(new SimpleMeterRegistry())
      }
      bind AnnotatedMetricService
    }

    handlers { MeterRegistry registry ->
      meterRegistry = registry
      path('meter') { AnnotatedMetricService service ->
        service
          .triggerMeter1()
          .triggerMeter2()
          .triggerTimer1()
          .triggerTimer2()
          .triggerTimer3()

        render ''
      }
    }

    when:
    2.times { get('meter') }

    then: '@Metered methods contribute to timers'
    meterRegistry?.get('foo.meter')?.timer()?.count() == 2
    meterRegistry?.get('ratpack.micrometer.metrics.internal.MicrometerMetricsModuleSpec$AnnotatedMetricService.triggerMeter2')
      ?.timer()?.count() == 2

    then: '@Timed methods contribute to timers'
    meterRegistry?.get('dropwizard.timed')?.timer()?.count() == 2
    meterRegistry?.get('micrometer.timed')?.timer()?.count() == 2
    meterRegistry?.get('ratpack.micrometer.metrics.internal.MicrometerMetricsModuleSpec$AnnotatedMetricService.triggerTimer2')
      ?.timer()?.count() == 2
  }

  def 'can properly capture timing events'() {
    given:
    MeterRegistry meterRegistry = null

    bindings {
      module new MicrometerMetricsModule(), {
        it.additionalMeterRegistries(new SimpleMeterRegistry())
      }
      bind AnnotatedMetricService
    }

    handlers { MeterRegistry registry ->
      meterRegistry = registry

      path('sync') { AnnotatedMetricService service ->
        render(service.triggerTimerSync())
      }

      path('async') { AnnotatedMetricService service ->
        render(service.triggerTimerPromise())
      }
    }

    when: 'Timing synchronous methods'
    def resultSync = get('sync')

    then: 'The synchronous methods should take at least the time they slept'
    meterRegistry?.get('foo.timer.sync')?.timer()?.totalTime(TimeUnit.MILLISECONDS) >= 50
    resultSync.body.text == 'resultSync'

    when: 'Timing promise methods'
    def resultPromise = get('async')

    then: 'The asynchronous methods should take at least the time they slept'
    meterRegistry?.get('foo.timer.promise')?.timer()?.totalTime(TimeUnit.MILLISECONDS) >= 50
    resultPromise.body.text == 'resultPromise'
  }

  def 'can collect gauge annotated metrics'() {
    given:
    MeterRegistry meterRegistry = null

    bindings {
      module new MicrometerMetricsModule(), {
        it.additionalMeterRegistries(new SimpleMeterRegistry())
      }
      bind AnnotatedMetricService
    }

    handlers { MeterRegistry registry ->
      meterRegistry = registry

      path('gauge') { AnnotatedMetricService service ->
        render ''
      }
    }

    when:
    get('gauge')

    then:
    meterRegistry?.get('foo.gauge')?.gauge()?.value() == 1.0d
    meterRegistry?.get('ratpack.micrometer.metrics.internal.MicrometerMetricsModuleSpec$AnnotatedMetricService.triggerGauge2')
      ?.gauge()?.value() == Double.NaN
  }

  def 'can collect request timer metrics'() {
    given:
    MeterRegistry meterRegistry = null

    bindings {
      module new MicrometerMetricsModule(), {
        it.additionalMeterRegistries(new SimpleMeterRegistry())
        it.addHandlerTags { context, throwable -> Tags.of('custom', 'mycustom') }
      }
    }

    handlers { MeterRegistry registry ->
      meterRegistry = registry

      prefix('foo') {
        path('bar') {
          render ''
        }
        path('baz/:id') { ctx ->
          render ctx.getPathTokens().get('id')
        }
        all {
          render ''
        }
      }
    }

    when:
    1.times { get('unroutable') }

    2.times { get('foo') }

    2.times { get('foo/bar') }
    2.times { get('foo/bar/') }
    2.times { get('foo/bar///') }
    2.times { get('/foo/bar///') }

    get('/foo/baz/123')

    then:
    meterRegistry?.get('http.server.requests')?.tag('uri', 'foo')?.tags(successfulGetTags)
      ?.tag('custom', 'mycustom')
      ?.timer()?.count() == 2
    meterRegistry?.get('http.server.requests')?.tag('uri', 'foo/bar')?.tags(successfulGetTags)?.timer()?.count() == 8
    meterRegistry?.get('http.server.requests')?.tag('uri', 'foo/baz/:id')?.tags(successfulGetTags)?.timer()?.count() == 1
    meterRegistry?.get('http.server.requests')?.tag('uri', 'NOT_FOUND')?.tags('outcome', 'CLIENT_ERROR')
      ?.timer()?.count() == 1
  }

  def 'can collect default bound metrics'() {
    given:
    MeterRegistry meterRegistry = null

    bindings {
      module new MicrometerMetricsModule(), {
        it.additionalMeterRegistries(new SimpleMeterRegistry())
      }
    }

    handlers { MeterRegistry registry ->
      meterRegistry = registry

      all {
        render ''
      }
    }

    when:
    get()

    then:
    meterRegistry.get('process.uptime').timeGauge()
  }

  def 'can collect blocking metrics'() {
    given:
    MeterRegistry meterRegistry = null

    bindings {
      module new MicrometerMetricsModule(), {
        it.additionalMeterRegistries(new SimpleMeterRegistry())
        it.addHandlerTags { context, throwable -> Tags.of('custom', 'mycustom') }
      }
    }

    handlers { MeterRegistry registry ->
      meterRegistry = registry

      path('foo') {
        Blocking.get {
          2
        } then {
          render ''
        }
      }
    }

    when:
    2.times { get('foo') }

    then:
    meterRegistry?.get('http.blocking.execution')
      ?.tags(successfulGetTags)
      ?.tag('custom', 'mycustom')
      ?.tag('uri', 'foo')
      ?.timer()?.count() == 2
  }
}
