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

import com.codahale.metrics.health.HealthCheck
import com.codahale.metrics.health.HealthCheckRegistry
import ratpack.test.internal.RatpackGroovyDslSpec

import javax.inject.Inject

class HealthchecksSpec extends RatpackGroovyDslSpec {

  @com.google.inject.Singleton
  static class SomeResource {
    boolean ok
  }

  static class MyHealthCheck extends NamedHealthCheck {

    @Inject
    SomeResource resource

    @SuppressWarnings("UnnecessaryQualifiedReference")
    protected HealthCheck.Result check() throws Exception {
      resource.ok ? HealthCheck.Result.healthy() : HealthCheck.Result.unhealthy("bad!")
    }

    def String getName() {
      "resource"
    }
  }

  static class FooHealthCheck extends NamedHealthCheck {

    @SuppressWarnings("UnnecessaryQualifiedReference")
    protected HealthCheck.Result check() throws Exception {
      HealthCheck.Result.healthy()
    }

    def String getName() {
      "foo"
    }
  }

  def "can register healthcheck"() {
    when:
    modules {
      register new CodaHaleMetricsModule()
      bind MyHealthCheck
    }
    handlers { HealthCheckRegistry healthChecks ->
      get {
        render healthChecks.runHealthChecks().toString()
      }
      post('toggle') {
        get(SomeResource).with { ok = !ok }
        response.send()
      }
    }

    then:
    text == "{resource=Result{isHealthy=false, message=bad!}}"

    when:
    post("toggle")

    then:
    text == "{resource=Result{isHealthy=true}}"
  }

  def "can use healthcheck endpoint"() {
    when:
    modules {
      register new CodaHaleMetricsModule()
      bind MyHealthCheck
      bind FooHealthCheck
    }
    handlers {
      prefix("admin") {
        handler(registry.get(HealthCheckEndpoint))
      }
    }

    then:
    getText("admin/health-check") == "{foo=Result{isHealthy=true}, resource=Result{isHealthy=false, message=bad!}}"
    getText("admin/health-check/foo") == "Result{isHealthy=true}"
  }

}

