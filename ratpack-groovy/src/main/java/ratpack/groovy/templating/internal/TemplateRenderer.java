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

package ratpack.groovy.templating.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import ratpack.file.MimeTypes;
import ratpack.func.Action;
import ratpack.groovy.templating.Template;
import ratpack.handling.Context;
import ratpack.render.RendererSupport;

import javax.inject.Inject;

import static ratpack.util.ExceptionUtils.toException;

public class TemplateRenderer extends RendererSupport<Template> {

  private final GroovyTemplateRenderingEngine engine;
  private final ByteBufAllocator byteBufAllocator;

  @Inject
  public TemplateRenderer(GroovyTemplateRenderingEngine engine, ByteBufAllocator byteBufAllocator) {
    this.engine = engine;
    this.byteBufAllocator = byteBufAllocator;
  }

  public void render(final Context context, final Template template) throws Exception {
    final ByteBuf buffer = byteBufAllocator.buffer();
    engine.renderTemplate(context, buffer, template.getId(), template.getModel()).onError(new Action<Throwable>() {
      @Override
      public void execute(Throwable thing) throws Exception {
        buffer.release();
        throw toException(thing);
      }
    }).then(new Action<ByteBuf>() {
      @Override
      public void execute(ByteBuf byteBuf) throws Exception {
        String type = template.getType();
        if (type == null) {
          type = context.get(MimeTypes.class).getContentType(template.getId());
        }
        context.getResponse().contentType(type).send(byteBuf);
      }
    });
  }
}
