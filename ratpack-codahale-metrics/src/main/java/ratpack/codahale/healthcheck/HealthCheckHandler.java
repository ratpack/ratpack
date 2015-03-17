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

package ratpack.codahale.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.collect.ImmutableSortedMap;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import java.util.NoSuchElementException;
import java.util.SortedMap;

/**
 * A Handler that runs and renders health checks.
 * <p>
 * This handler should be bound to an application path, and most likely only for the GET method…
 * <pre class="java-chain-dsl">
 * import ratpack.codahale.healthcheck.HealthCheckHandler;
 * import static org.junit.Assert.*;
 *
 * assertTrue(chain instanceof ratpack.handling.Chain);
 * chain.get("health-checks/:name?", new HealthCheckHandler());
 * </pre>
 * <p>
 * The handler can render the result of all of the health checks or an individual health check, depending on the presence of a path token.
 * The path token provides the name of the health check to render.
 * If the path token is not present, all health checks will be rendered.
 * The token name to use can be provided as the construction argument to this handler.
 * The default token name is {@value #DEFAULT_HEALTH_CHECK_NAME_TOKEN} and is used if the no-arg constructor is used.
 * <p>
 * If the token is present, the health check whose name is the value of the token will be rendered.
 * If no health check exists by that name, the client will receive a 404.
 * <p>
 * When a single health check is selected (by presence of the path token) the corresponding {@link com.codahale.metrics.health.HealthCheck.Result} is {@link Context#render(Object) rendered}.
 * When rendering all health checks a {@link HealthCheckResults} is {@link Context#render(Object) rendered}.
 * The {@link CodaHaleHealthCheckModule} installs renderers for these two types that simply renders to the {@code toString()} as plain text.
 * If you wish to change the representation, to JSON for example, you can register your own renderers for these types after registering the
 * {@link CodaHaleHealthCheckModule}.
 */
public class HealthCheckHandler implements Handler {

  /**
   * The default path token used to identify a particular health check: {@value}
   *
   * @see #HealthCheckHandler()
   */
  public static final String DEFAULT_HEALTH_CHECK_NAME_TOKEN = "name";

  private final String healthCheckNameToken;

  public HealthCheckHandler() {
    this(DEFAULT_HEALTH_CHECK_NAME_TOKEN);
  }

  public HealthCheckHandler(String healthCheckNameToken) {
    this.healthCheckNameToken = healthCheckNameToken;
  }

  @Override
  public void handle(Context context) throws Exception {

    // TODO: We should consider running health checks on a non request thread, which would allow them to block.

    HealthCheckRegistry registry = context.get(HealthCheckRegistry.class);
    String healthCheckName = context.getPathTokens().get(healthCheckNameToken);

    if (healthCheckName != null) {
      HealthCheck.Result result;
      try {
        result = registry.runHealthCheck(healthCheckName);
      } catch (NoSuchElementException e) {
        result = null;
      }

      if (result == null) {
        context.clientError(404);
      } else {
        context.render(result);
      }
    } else {
      SortedMap<String, HealthCheck.Result> healthCheckResults = registry.runHealthChecks();
      HealthCheckResults wrappedHealthCheckResults = new HealthCheckResults(ImmutableSortedMap.copyOfSorted(healthCheckResults));
      context.render(wrappedHealthCheckResults);
    }
  }

}
