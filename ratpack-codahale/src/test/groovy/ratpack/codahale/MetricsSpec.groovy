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

import com.codahale.metrics.Meter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.MetricRegistryListener
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ratpack.test.internal.RatpackGroovyDslSpec

class MetricsSpec extends RatpackGroovyDslSpec {

  @Rule
  TemporaryFolder reportDirectory

  def "can register metrics module"() {
    when:
    app {
      modules {
        register new CodaHaleModule()
      }
      handlers { MetricRegistry metrics ->
        handler {
          render metrics.getClass().name
        }
      }
    }

    then:
    text == "com.codahale.metrics.MetricRegistry"
  }

  def "can register reporters"() {
    when:
    List<MetricRegistryListener> listeners = null

    app {
      modules {
        register new CodaHaleModule().jmx(true).csv(reportDirectory.root)
      }
      handlers { MetricRegistry metrics ->
        handler {
          //noinspection GroovyAccessibility
          listeners = metrics.listeners
          render "ok"
        }
      }
    }

    then:
    text == "ok"
    listeners.size() == 1 // JMX listener
    sleep 1100 // csv reporter polls every second - TODO use a spin assert
    reportDirectory.root.listFiles().length > 0
  }

  def "can collect custom metrics"() {
    def reporter = Mock(MetricRegistryListener)
    Meter requests = null

    given:
    app {
      modules {
        register new CodaHaleModule()
      }

      handlers { MetricRegistry metrics ->
        // TODO this is a bad place to do this - We should auto register user added MetricRegistryListeners
        // Also need to consider a more general post startup hook
        requests = metrics.meter("requests")
        metrics.addListener(reporter)

        handler {
          metrics.meter("requests").mark()
          render ""
        }
      }
    }

    when:
    2.times { get() }

    then:
    1 * reporter.onMeterAdded("requests", !null)
    requests.count == 2
  }

  def "can collect request timer metrics"() {
    def reporter = Mock(MetricRegistryListener)

    given:
    app {
      modules {
        register new CodaHaleModule().jmx(true)
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

}
