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

package org.ratpackframework.groovy.templating.internal;

import io.netty.buffer.ByteBuf;
import org.ratpackframework.file.MimeTypes;
import org.ratpackframework.groovy.Template;
import org.ratpackframework.handling.Context;
import org.ratpackframework.render.ByTypeRenderer;
import org.ratpackframework.util.internal.Result;
import org.ratpackframework.util.internal.ResultAction;

import javax.inject.Inject;

public class TemplateRenderer extends ByTypeRenderer<Template> {

  private final GroovyTemplateRenderingEngine engine;

  @Inject
  public TemplateRenderer(GroovyTemplateRenderingEngine engine) {
    super(Template.class);
    this.engine = engine;
  }

  public void render(final Context context, final Template template) {
    engine.renderTemplate(template.getId(), template.getModel(), new ResultAction<ByteBuf>() {
      public void execute(Result<ByteBuf> thing) {
        if (thing.isFailure()) {
          context.error(thing.getFailure());
        } else {
          String type = template.getType();
          if (type == null) {
            type = context.get(MimeTypes.class).getContentType(template.getId());
          }

          context.getResponse().send(type, thing.getValue());
        }
      }
    });
  }
}
