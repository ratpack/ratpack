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

package ratpack.health;

import ratpack.handling.Context;
import ratpack.render.RendererSupport;

/**
 * A renderer for results of non-blocking health checks used by {@link ratpack.handling.Context#render(Object) renderable}
 * <p>
 * Renders in plain text in the following format
 * <pre>{@code
 *  name : HEALTHY|UNHEALTHY [message] [exception]
 * }</pre>
 * <p>
 * Renderer sets no caching HTTP pragmas on {@code Context#getResponse()} object.
 * <p>
 * Renderer is automatically added to <strong>Ratpack's</strong> base registry.
 *
 * @see ratpack.health.HealthCheckResults
 * @see ratpack.health.HealthCheckHandler
 * @see ratpack.handling.Context
 */
public class HealthCheckResultsRenderer extends RendererSupport<HealthCheckResults> {
  @Override
  public void render(Context context, HealthCheckResults healthCheckResults) throws Exception {
    if (healthCheckResults == null) {
      context.clientError(405);
      return;
    }
    StringBuilder builder = new StringBuilder();
    healthCheckResults.getResults().forEach((name, result) -> {
      builder.append(name).append(" : ").append(result.isHealthy() ? "HEALTHY" : "UNHEALTHY");
      if (!result.isHealthy()) {
        builder.append(" [").append(result.getMessage()).append(" ]");
        if (result.getError() != null) {
          builder.append(" [").append(result.getError().toString()).append(" ]");
        }
      }
      builder.append("\n");
    });
    context.getResponse().getHeaders()
            .add("Cache-Control", "no-cache, no-store, must-revalidate")
            .add("Pragma", "no-cache")
            .add("Expires", 0);
    context.getResponse().send(builder.toString());
  }
}
