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
import ratpack.health.HealthCheckResults;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.render.Renderer;
import ratpack.render.RendererSupport;
import ratpack.util.Types;

import java.io.BufferedOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class HealthCheckResultsRenderer extends RendererSupport<HealthCheckResults> {

  public static final TypeToken<Renderer<HealthCheckResults>> TYPE = Types.intern(new TypeToken<Renderer<HealthCheckResults>>() {});

  private final ByteBufAllocator byteBufAllocator;

  public HealthCheckResultsRenderer(ByteBufAllocator byteBufAllocator) {
    this.byteBufAllocator = byteBufAllocator;
  }

  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  @Override
  public void render(Context ctx, HealthCheckResults healthCheckResults) throws Exception {
    ByteBuf buffer = byteBufAllocator.buffer();

    try (Writer writer = new OutputStreamWriter(new BufferedOutputStream(new ByteBufOutputStream(buffer)))) {
      healthCheckResults.writeTo(writer);
    } catch (Exception e) {
      buffer.release();
      throw e;
    }

    ctx.getResponse()
      .contentTypeIfNotSet(HttpHeaderConstants.PLAIN_TEXT_UTF8)
      .status(healthCheckResults.isUnhealthy() ? 503 : 200)
      .send(buffer);
  }
}
