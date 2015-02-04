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

package ratpack.codahale.healthcheck;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.reflect.TypeToken;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import ratpack.codahale.healthcheck.internal.HealthCheckResultRenderer;
import ratpack.codahale.healthcheck.internal.HealthCheckResultsRenderer;
import ratpack.func.Action;
import ratpack.guice.internal.GuiceUtil;
import ratpack.server.Service;
import ratpack.server.StartEvent;

import javax.inject.Inject;

import static com.google.inject.Scopes.SINGLETON;

/**
 * An extension module that provides support for health checks via Coda Hale's Metrics library.
 * <p>
 * Health checks verify that application components or responsibilities are performing as expected.
 * To use this module one has to register the module and create health checks by simply creating a class that extends {@link NamedHealthCheck} and bind it with Guice.
 * This will automatically add it to the application wide {@link com.codahale.metrics.health.HealthCheckRegistry}.
 * <p>
 * Health checks can be run by obtaining the {@link com.codahale.metrics.health.HealthCheckRegistry} via dependency injection or context registry lookup,
 * then calling {@link com.codahale.metrics.health.HealthCheckRegistry#runHealthChecks()}.
 * <p>
 * To expose the health check status over HTTP, see {@link HealthCheckHandler}.
 * <p>
 * Example health checks: (Groovy DSL)
 * </p>
 * <pre class="groovy-ratpack-dsl">
 * import com.codahale.metrics.health.HealthCheck
 * import com.codahale.metrics.health.HealthCheckRegistry
 * import ratpack.codahale.healthcheck.CodaHaleHealthCheckModule
 * import ratpack.codahale.healthcheck.HealthCheckHandler
 * import ratpack.codahale.healthcheck.NamedHealthCheck
 * import static ratpack.groovy.Groovy.ratpack
 *
 * class FooHealthCheck extends NamedHealthCheck {
 *
 *   protected HealthCheck.Result check() throws Exception {
 *     // perform the health check logic here and return HealthCheck.Result.healthy() or HealthCheck.Result.unhealthy("Unhealthy message")
 *     HealthCheck.Result.healthy()
 *   }
 *
 *   def String getName() {
 *     "foo_health_check"
 *   }
 * }
 *
 * ratpack {
 *   bindings {
 *     add new CodaHaleHealthCheckModule()
 *     bind FooHealthCheck // if you don't bind the health check with Guice it will not be automatically registered
 *   }
 *
 *   handlers {
 *     // Using the provided handler…
 *     get("health-check/:name", new HealthCheckHandler())
 *
 *     // Using a custom handler to run all health checks…
 *     get("healthChecks-custom") { HealthCheckRegistry healthCheckRegistry -&gt;
 *       render healthCheckRegistry.runHealthChecks().toString()
 *     }
 *   }
 * }
 * </pre>
 *
 * @see <a href="https://dropwizard.github.io/metrics/3.1.0/manual/healthchecks/" target="_blank">Coda Hale Metrics - Health Checks</a>
 */
public class CodaHaleHealthCheckModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(HealthCheckRegistry.class).in(SINGLETON);
    bind(HealthCheckResultRenderer.class).in(SINGLETON);
    bind(HealthCheckResultsRenderer.class).in(SINGLETON);

    bind(Startup.class);
  }

  private static class Startup implements Service {
    private final Injector injector;

    @Inject
    public Startup(Injector injector) {
      this.injector = injector;
    }

    @Override
    public void onStart(StartEvent event) throws Exception {
      final HealthCheckRegistry registry = injector.getInstance(HealthCheckRegistry.class);
      GuiceUtil.eachOfType(injector, TypeToken.of(NamedHealthCheck.class), new Action<NamedHealthCheck>() {
        public void execute(NamedHealthCheck healthCheck) throws Exception {
          registry.register(healthCheck.getName(), healthCheck);
        }
      });
    }
  }

}
