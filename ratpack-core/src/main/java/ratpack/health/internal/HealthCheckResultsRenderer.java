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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import ratpack.handling.Context;
import ratpack.health.HealthCheck;
import ratpack.health.HealthCheckResults;
import ratpack.render.RendererSupport;

import java.io.BufferedOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;

public class HealthCheckResultsRenderer extends RendererSupport<HealthCheckResults> {

  private final ByteBufAllocator byteBufAllocator;

  public HealthCheckResultsRenderer(ByteBufAllocator byteBufAllocator) {
    this.byteBufAllocator = byteBufAllocator;
  }

  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  @Override
  public void render(Context context, HealthCheckResults healthCheckResults) throws Exception {
    ByteBuf buffer = byteBufAllocator.buffer();
    boolean first = true;
    boolean unhealthy = false;

    try {
      Writer writer = new OutputStreamWriter(new BufferedOutputStream(new ByteBufOutputStream(buffer)));
      for (Map.Entry<String, HealthCheck.Result> entry : healthCheckResults.getResults().entrySet()) {
        if (first) {
          first = false;
        } else {
          writer.write("\n");
        }
        String name = entry.getKey();
        HealthCheck.Result result = entry.getValue();
        writer.append(name).append(" : ").append(result.isHealthy() ? "HEALTHY" : "UNHEALTHY");
        if (!result.isHealthy()) {
          unhealthy = true;
          writer.append(" [").append(result.getMessage()).append("]");
          if (result.getError() != null) {
            writer.append(" [").append(result.getError().toString()).append("]");
          }
        }
      }
      writer.close();
    } catch (Exception e) {
      buffer.release();
      throw e;
    }

    context.getResponse().status(unhealthy ? 503 : 200).send(buffer);
  }
}
