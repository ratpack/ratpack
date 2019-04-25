/*
 * Copyright 2018 the original author or authors.
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
package ratpack.micrometer

import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.netty.handler.codec.http.HttpResponseStatus
import ratpack.groovy.test.handling.GroovyRequestFixture
import ratpack.test.handling.HandlingResult
import spock.lang.Specification

class PrometheusHandlerShould extends Specification {

  def "display metrics"() {
    given:
    PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    registry.counter("aCounter").increment()

    when:
    HandlingResult result = GroovyRequestFixture.handle(new PrometheusHandler(registry)) {}

    then:
    result.status.code == HttpResponseStatus.OK.code()
    result.bodyText.contains("aCounter_total 1.0")
  }
}
