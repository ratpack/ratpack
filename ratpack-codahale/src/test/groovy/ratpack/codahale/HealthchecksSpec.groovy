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

import com.codahale.metrics.health.HealthCheck.Result
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
    protected Result check() throws Exception {
      resource.ok ? Result.healthy() : Result.unhealthy("bad!")
    }

    def String getName() {
      "resource"
    }
  }

  def "can register healthcheck"() {
    when:
    app {
      modules {
        register new MetricsModule()
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
    }

    then:
    text == "{resource=Result{isHealthy=false, message=bad!}}"

    when:
    post("toggle")

    then:
    text == "{resource=Result{isHealthy=true}}"
  }

}

