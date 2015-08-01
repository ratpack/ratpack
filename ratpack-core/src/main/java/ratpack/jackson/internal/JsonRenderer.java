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

package ratpack.jackson.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import ratpack.handling.Context;
import ratpack.http.internal.HttpHeaderConstants;
import ratpack.jackson.JsonRender;
import ratpack.render.RendererSupport;

import java.io.OutputStream;

public class JsonRenderer extends RendererSupport<JsonRender> {

  private final ObjectWriter defaultObjectWriter;

  public JsonRenderer(ObjectWriter defaultObjectWriter) {
    this.defaultObjectWriter = defaultObjectWriter;
  }

  @Override
  public void render(Context context, JsonRender object) throws Exception {
    ObjectWriter writer = object.getObjectWriter();
    if (writer == null) {
      writer = defaultObjectWriter;
    }
    Class<?> viewClass = object.getViewClass();
    if(viewClass != null) {
      writer = writer.withView(viewClass);
    }

    ByteBuf buffer = context.get(ByteBufAllocator.class).buffer();
    OutputStream outputStream = new ByteBufOutputStream(buffer);

    try {
      writer.writeValue(outputStream, object.getObject());
    } catch (JsonProcessingException e) {
      buffer.release();
      context.error(e);
      return;
    }

    context.getResponse()
      .contentTypeIfNotSet(HttpHeaderConstants.JSON)
      .send(buffer);
  }

}
