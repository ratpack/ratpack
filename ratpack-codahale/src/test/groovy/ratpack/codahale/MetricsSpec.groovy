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

package ratpack.codahale

import com.codahale.metrics.*
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ratpack.test.internal.RatpackGroovyDslSpec
import spock.lang.Stepwise

import java.lang.reflect.Field
import java.util.concurrent.ExecutorService

@Stepwise // TODO - rewrite these tests so they are not order dependent
class MetricsSpec extends RatpackGroovyDslSpec {

  @Rule TemporaryFolder reportDirectory

  def "can register metrics module"() {
    MetricsModule metricsModule = new MetricsModule()
    modules << metricsModule

    when:
    app {
      handlers { MetricRegistry metrics ->
        handler {
          render metrics.getClass().name
        }
      }
    }

    then:
    getText() == "com.codahale.metrics.MetricRegistry"
  }

  def "can register metrics module with jmx reporter"() {
    MetricsModule metricsModule = new MetricsModule().reportToJmx()
    modules << metricsModule

    given:
    app {
      handlers { MetricRegistry metrics ->
        handler {
          render ""
        }
      }
    }

    when:
    get()

    then:
    with (metricsModule.registry) {
      listeners.size() == 1
      listeners[0].class.name == 'com.codahale.metrics.JmxReporter$JmxListener'
    }

    cleanup:
    metricsModule.registry.removeListener(metricsModule.registry.listeners[0])
  }

  def "can stop jmx reporter"() {
    MetricsModule metricsModule = new MetricsModule().reportToJmx()
    modules << metricsModule

    given:
    app {
      handlers { MetricRegistry metrics, JmxReporter jmxReporter ->
        handler {
          jmxReporter.stop()
          render ""
        }
      }
    }

    when:
    get()

    then:
    metricsModule.registry.listeners.size() == 0
  }

  def "can register metrics module with csv reporter"() {
    MetricsModule metricsModule = new MetricsModule().reportToCsv(reportDirectory.root)
    modules << metricsModule
    CsvReporter reporter = null

    given:
    app {
      handlers { MetricRegistry metrics, CsvReporter csvReporter ->
        handler {
          reporter = csvReporter
          render ""
        }
      }

    }

    when:
    get()

    then:
    metricsModule.registry.listeners.size() == 0

    Field field = ScheduledReporter.getDeclaredField("executor");
    field.setAccessible(true);
    ExecutorService executor = field.get(reporter);
    !executor.isShutdown()
  }

  def "can stop csv reporter"() {
    MetricsModule metricsModule = new MetricsModule().reportToCsv(reportDirectory.root)
    modules << metricsModule
    CsvReporter reporter = null

    given:
    app {
      handlers { MetricRegistry metrics, CsvReporter csvReporter ->
        handler {
          reporter = csvReporter
          csvReporter.stop()
          render ""
        }
      }

    }

    when:
    get()

    then:
    Field field = ScheduledReporter.getDeclaredField("executor");
    field.setAccessible(true);
    ExecutorService executor = field.get(reporter);
    executor.isShutdown()
  }

  def "can register metrics module with multiple reporters"() {
    MetricsModule metricsModule = new MetricsModule().reportToCsv(reportDirectory.root).reportToJmx()
    modules << metricsModule
    CsvReporter reporter = null

    given:
    app {
      handlers { MetricRegistry metrics, CsvReporter csvReporter ->
        handler {
          reporter = csvReporter
          render ""
        }
      }
    }

    when:
    get()

    then:
    with (metricsModule.registry) {
      listeners.size() == 1
      listeners[0].class.name == 'com.codahale.metrics.JmxReporter$JmxListener'
    }

    Field field = ScheduledReporter.getDeclaredField("executor");
    field.setAccessible(true);
    ExecutorService executor = field.get(reporter);
    !executor.isShutdown()

    cleanup:
    metricsModule.registry.removeListener(metricsModule.registry.listeners[0])
  }

  def "can collect custom metrics"() {
    MetricsModule metricsModule = new MetricsModule()
    modules << metricsModule

    MetricRegistryListener reporter = Mock()
    Meter requests = null

    given:
    app {
      modules {
        requests = get(MetricsModule).registry.meter("requests")
        get(MetricsModule).registry.addListener(reporter)
      }

      handlers { MetricRegistry metrics ->
        handler {
          metrics.meter("requests").mark()
          render ""
        }
      }
    }

    when:
    2.times {get()}

    then:
    1 * reporter.onMeterAdded("requests", !null)
    requests.count == 2
  }

  def "can collect request timer metrics"() {
    MetricsModule metricsModule = new MetricsModule()
    modules << metricsModule

    MetricRegistryListener reporter = Mock()

    given:
    app {
      modules {
        get(MetricsModule).registry.addListener(reporter)
      }

      handlers { MetricRegistry metrics ->
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
    }

    when:
    2.times {get()}
    2.times {get("foo")}
    2.times {get("foo/bar")}

    then:
    1 * reporter.onTimerAdded("[root]~GET~Request", !null)
    1 * reporter.onTimerAdded("[foo]~GET~Request", !null)
    1 * reporter.onTimerAdded("[foo][bar]~GET~Request", !null)
  }

}
