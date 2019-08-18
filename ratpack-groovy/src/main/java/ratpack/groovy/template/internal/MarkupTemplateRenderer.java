/*
 * Copyright 2014 the original author or authors.
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
package ratpack.groovy.template.internal;

import groovy.lang.Writable;
import groovy.text.Template;
import groovy.text.markup.MarkupTemplateEngine;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.util.CharsetUtil;
import ratpack.file.MimeTypes;
import ratpack.groovy.template.MarkupTemplate;
import ratpack.handling.Context;
import ratpack.render.RendererSupport;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

public class MarkupTemplateRenderer extends RendererSupport<MarkupTemplate> {

  private final MarkupTemplateEngine engine;
  private final ByteBufAllocator byteBufAllocator;

  @Inject
  public MarkupTemplateRenderer(MarkupTemplateEngine engine, ByteBufAllocator byteBufAllocator) {
    this.engine = engine;
    this.byteBufAllocator = byteBufAllocator;
  }

  @Override
  public void render(Context ctx, MarkupTemplate template) throws Exception {
    String contentType = template.getContentType();
    contentType = contentType == null ? ctx.get(MimeTypes.class).getContentType(template.getName()) : contentType;

    try {
      Template compiledTemplate = engine.createTemplateByPath(template.getName());
      Writable boundTemplate = compiledTemplate.make(template.getModel());

      ByteBuf byteBuf = byteBufAllocator.directBuffer();
      try {
        OutputStream outputStream = new ByteBufOutputStream(byteBuf);
        Writer writer = new OutputStreamWriter(outputStream, CharsetUtil.encoder(StandardCharsets.UTF_8));
        boundTemplate.writeTo(writer);
      } catch (Exception e) {
        byteBuf.release();
        throw e;
      }

      ctx.getResponse().send(contentType, byteBuf);
    } catch (IOException e) {
      ctx.error(e);
    }
  }
}
