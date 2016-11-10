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

import com.google.common.reflect.TypeToken;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import ratpack.handling.Context;
import ratpack.health.HealthCheck;
import ratpack.health.HealthCheckResults;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.registry.internal.TypeCaching;
import ratpack.render.Renderer;
import ratpack.render.RendererSupport;

import java.io.BufferedOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;

public class HealthCheckResultsRenderer extends RendererSupport<HealthCheckResults> {

  public static final TypeToken<Renderer<HealthCheckResults>> TYPE = TypeCaching.typeToken(new TypeToken<Renderer<HealthCheckResults>>() {});

  private final ByteBufAllocator byteBufAllocator;

  public HealthCheckResultsRenderer(ByteBufAllocator byteBufAllocator) {
    this.byteBufAllocator = byteBufAllocator;
  }

  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  @Override
  public void render(Context ctx, HealthCheckResults healthCheckResults) throws Exception {
    ByteBuf buffer = byteBufAllocator.buffer();
    boolean first = true;
    boolean unhealthy = false;

    try {
      try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new ByteBufOutputStream(buffer)))) {
        for (Map.Entry<String, HealthCheck.Result> entry : healthCheckResults.getResults().entrySet()) {
          if (first) {
            first = false;
          } else {
            writer.write("\n");
          }

          String name = entry.getKey();
          HealthCheck.Result result = entry.getValue();

          unhealthy = unhealthy || !result.isHealthy();

          writer.append(name).append(" : ").append(result.isHealthy() ? "HEALTHY" : "UNHEALTHY");

          String message = result.getMessage();
          if (message != null) {
            writer.append(" [").append(message).append("]");
          }

          Throwable error = result.getError();
          if (error != null) {
            writer.append(" [").append(error.toString()).append("]");
          }
        }
      }
    } catch (Exception e) {
      buffer.release();
      throw e;
    }

    ctx.getResponse()
      .contentTypeIfNotSet(HttpHeaderConstants.PLAIN_TEXT_UTF8)
      .status(unhealthy ? 503 : 200)
      .send(buffer);
  }
}
