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

package ratpack.codahale.healthcheck

import com.codahale.metrics.health.HealthCheck
import com.codahale.metrics.health.HealthCheckRegistry
import ratpack.handling.Context
import ratpack.jackson.JacksonModule
import ratpack.render.RendererSupport
import ratpack.test.internal.RatpackGroovyDslSpec

import javax.inject.Inject
import javax.inject.Singleton

import static ratpack.jackson.Jackson.json

class HealthchecksSpec extends RatpackGroovyDslSpec {

  @Singleton
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
    bindings {
      add new CodaHaleHealthCheckModule()
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
    bindings {
      add new CodaHaleHealthCheckModule()
      bind MyHealthCheck
      bind FooHealthCheck
    }
    handlers {
      get("admin/health-check/:name?", new HealthCheckHandler())
    }

    then:
    getText("admin/health-check") == '{"results":{"foo":{"healthy":true,"message":null,"error":null},"resource":{"healthy":false,"message":"bad!","error":null}}}'
    getText("admin/health-check/foo") == '{"healthy":true,"message":null,"error":null}'
  }

  def "non existent health check returns 404"() {
    when:
    bindings {
      add new CodaHaleHealthCheckModule()
      bind MyHealthCheck
      bind FooHealthCheck
    }
    handlers {
      get("admin/health-check/:name?", new HealthCheckHandler())
    }

    then:
    get("admin/health-check/bar")
    response.statusCode == 404
  }

  static class HealthCheckJsonRenderer extends RendererSupport<HealthCheck.Result> {
    @Override
    void render(Context context, HealthCheck.Result object) throws Exception {
      context.render(json(object))
    }
  }

  static class HealthChecksJsonRenderer extends RendererSupport<HealthCheckResults> {
    @Override
    void render(Context context, HealthCheckResults object) throws Exception {
      context.render(json(object.getResults()))
    }
  }

  def "can use healthcheck endpoint with custom renderer"() {
    when:
    bindings {
      add new CodaHaleHealthCheckModule()
      add JacksonModule, { it.prettyPrint(false) }
      bind MyHealthCheck
      bind FooHealthCheck
      bind HealthCheckJsonRenderer
      bind HealthChecksJsonRenderer
    }
    handlers {
      get("admin/health-check/:name?", new HealthCheckHandler())
    }

    then:
    getText("admin/health-check") == '{"foo":{"healthy":true,"message":null,"error":null},"resource":{"healthy":false,"message":"bad!","error":null}}'
    getText("admin/health-check/foo") == '{"healthy":true,"message":null,"error":null}'
  }

}
