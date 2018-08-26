/*
 * Copyright 2018 the original author or authors.
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

package ratpack.gson.internal;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import ratpack.gson.GsonRender;
import ratpack.handling.Context;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.render.Renderer;
import ratpack.render.RendererSupport;
import ratpack.util.Types;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class GsonRenderer extends RendererSupport<GsonRender> {

  public static final TypeToken<Renderer<GsonRender>> TYPE = Types.intern(new TypeToken<Renderer<GsonRender>>() {
  });

  public static final Renderer<GsonRender> INSTANCE = new GsonRenderer();

  @Override
  public void render(Context ctx, GsonRender object) throws Exception {
    Gson gson = object.getGson();
    if (gson == null) {
      gson = ctx.maybeGet(Gson.class)
        .orElseGet(() -> new GsonBuilder().create());
    }

    ByteBuf buffer = ctx.get(ByteBufAllocator.class).buffer();
    OutputStream outputStream = new ByteBufOutputStream(buffer);
    Writer writer = new OutputStreamWriter(outputStream);

    try {
      gson.toJson(object.getObject(), writer);
      writer.flush();
    } catch (JsonIOException e) {
      buffer.release();
      ctx.error(e);
      return;
    }

    ctx.getResponse()
      .contentTypeIfNotSet(HttpHeaderConstants.JSON)
      .send(buffer);
  }
}
