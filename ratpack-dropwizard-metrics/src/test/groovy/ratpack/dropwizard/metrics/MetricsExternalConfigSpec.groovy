/*
 * Copyright 2015 the original author or authors.
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

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ratpack.config.ConfigData
import ratpack.test.internal.RatpackGroovyDslSpec

import java.time.Duration

class MetricsExternalConfigSpec extends RatpackGroovyDslSpec {

  @Rule
  TemporaryFolder tempFolder

  def "can explicitly disable a reporter"() {
    given:
    def propsFile = tempFolder.newFile("application.properties").toPath()
    propsFile.text = """
    |metrics.jvmMetrics=true
    |metrics.jmx.enabled=false
    |""".stripMargin()

    def config = ConfigData.of { c -> c.props(propsFile) }
    def metricsConfig = config.get("/metrics", DropwizardMetricsConfig)

    expect:
    metricsConfig.isJvmMetrics()
    metricsConfig.jmx.isPresent()
    !metricsConfig.jmx.get().enabled
    !metricsConfig.csv.isPresent()
    !metricsConfig.console.isPresent()
    !metricsConfig.webSocket.isPresent()
    !metricsConfig.slf4j.isPresent()
  }

  def "can explicitly enable a reporter"() {
    given:
    def propsFile = tempFolder.newFile("application.properties").toPath()
    propsFile.text = """
    |metrics.console.enabled=true
    |""".stripMargin()

    def config = ConfigData.of { c -> c.props(propsFile) }
    def metricsConfig = config.get("/metrics", DropwizardMetricsConfig)

    expect:
    !metricsConfig.isJvmMetrics()
    !metricsConfig.jmx.isPresent()
    !metricsConfig.csv.isPresent()
    metricsConfig.console.isPresent()
    metricsConfig.console.get().enabled
    !metricsConfig.webSocket.isPresent()
    !metricsConfig.slf4j.isPresent()
  }

  def "can implicitly enable a reporter by setting other config options"() {
    given:
    def propsFile = tempFolder.newFile("application.properties").toPath()
    propsFile.text = """
    |metrics.csv.reporterInterval=PT15M
    |""".stripMargin()

    def config = ConfigData.of { c -> c.props(propsFile) }
    def metricsConfig = config.get("/metrics", DropwizardMetricsConfig)

    expect:
    !metricsConfig.isJvmMetrics()
    !metricsConfig.jmx.isPresent()
    metricsConfig.csv.isPresent()
    metricsConfig.csv.get().enabled
    metricsConfig.csv.get().reporterInterval == Duration.ofMinutes(15)
    !metricsConfig.console.isPresent()
    !metricsConfig.webSocket.isPresent()
  }

  def "can explicitly disable a reporter when setting other config options"() {
    given:
    def propsFile = tempFolder.newFile("application.properties").toPath()
    propsFile.text = """
    |metrics.csv.reportDirectory=/foo
    |metrics.csv.enabled=false
    |""".stripMargin()

    def config = ConfigData.of { c -> c.props(propsFile) }
    def metricsConfig = config.get("/metrics", DropwizardMetricsConfig)

    expect:
    !metricsConfig.isJvmMetrics()
    !metricsConfig.jmx.isPresent()
    metricsConfig.csv.isPresent()
    !metricsConfig.csv.get().enabled
    metricsConfig.csv.get().reportDirectory.absolutePath == new File('/foo').absolutePath
    !metricsConfig.console.isPresent()
    !metricsConfig.webSocket.isPresent()
  }

  def "can not enable a reporter with with no property value"() {
    given:
    def propsFile = tempFolder.newFile("application.properties").toPath()
    propsFile.text = """
    |metrics.jmx
    |""".stripMargin()

    and:
    def config = ConfigData.of { c -> c.props(propsFile) }

    when:
    config.get("/metrics", DropwizardMetricsConfig)

    then:
    thrown UncheckedIOException
  }

  def "can enable and disable request timing handler and blocking execution timing interceptor"() {
    given:
    def propsFile = tempFolder.newFile("application.properties").toPath()
    propsFile.text = """
    |metrics.requestTimingMetrics=${value}
    |metrics.blockingTimingMetrics=${value}
    |""".stripMargin()

    and:
    def config = ConfigData.of { c -> c.props(propsFile) }

    when:
    def metricsConfig = config.get("/metrics", DropwizardMetricsConfig)

    then:
    metricsConfig.isRequestTimingMetrics() == value
    metricsConfig.isBlockingTimingMetrics() == value

    where:
    value << [ true, false ]
  }

  def "appropriate defaults for request timing handler and blocking execution timing interceptor"() {
    given:
    def propsFile = tempFolder.newFile("application.properties").toPath()
    propsFile.text = """
    |""".stripMargin()

    and:
    def config = ConfigData.of { c -> c.props(propsFile) }

    when:
    def metricsConfig = config.get("/metrics", DropwizardMetricsConfig)

    then:
    metricsConfig.isRequestTimingMetrics()
    metricsConfig.isBlockingTimingMetrics()
  }

}
