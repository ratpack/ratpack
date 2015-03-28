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

package ratpack.health.internal;

import ratpack.handling.Context;
import ratpack.health.HealthCheckResults;
import ratpack.render.RendererSupport;

public class HealthCheckResultsRenderer extends RendererSupport<HealthCheckResults> {
  @Override
  public void render(Context context, HealthCheckResults healthCheckResults) throws Exception {
    StringBuilder builder = new StringBuilder();
    healthCheckResults.getResults().forEach((name, result) -> {
      if (builder.length() > 0) {
        builder.append("\n");
      }
      builder.append(name).append(" : ").append(result.isHealthy() ? "HEALTHY" : "UNHEALTHY");
      if (!result.isHealthy()) {
        builder.append(" [").append(result.getMessage()).append("]");
        if (result.getError() != null) {
          builder.append(" [").append(result.getError().toString()).append("]");
        }
      }
    });

    context.getResponse().send(builder.toString());
  }
}
