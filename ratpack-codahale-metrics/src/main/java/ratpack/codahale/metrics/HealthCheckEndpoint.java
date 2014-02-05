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

package ratpack.codahale.metrics;

import com.codahale.metrics.health.HealthCheckRegistry;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.path.PathBinder;
import ratpack.path.internal.PathHandler;

/**
 * A Handler implementation for running health checks.
 * <p>
 * The Handler is constructed with a {@link PathBinder} and has an optional <code>name</code> token.
 * All health checks or a specific health check is run depending on whether the <code>name</code>
 * token is provided or not.
 * <p>
 * For exmaple, if the Handler is bound to the path <code>health-check</code> then...
 * <p>
 * All health checks can be run by sending a request to <code>/health-check</code>
 * <p>
 * A health check called <code>foo</code> can be run by sending a request to <code>/health-check/foo</code>
 *
 * @see ratpack.codahale.metrics.CodaHaleMetricsModule#healthChecks()
 */
public class HealthCheckEndpoint extends PathHandler {

  public HealthCheckEndpoint(PathBinder binding) {
    super(binding, new Handler() {

      @Override
      public void handle(final Context context) throws Exception {
        context.respond(context.getByMethod().
          get(new Runnable() {
            public void run() {
              HealthCheckRegistry registry = context.get(HealthCheckRegistry.class);
              String healthCheckName = context.getPathTokens().get("name");

              if (healthCheckName != null) {
                context.render(registry.runHealthCheck(healthCheckName).toString());
              } else {
                context.render(registry.runHealthChecks().toString());
              }
            }
          })
        );
      }

    });
  }

}

