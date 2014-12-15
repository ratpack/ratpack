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

import ratpack.file.MimeTypes;
import ratpack.groovy.templating.Template;
import ratpack.handling.Context;
import ratpack.render.RendererSupport;

import javax.inject.Inject;

public class TemplateRenderer extends RendererSupport<Template> {

  private final GroovyTemplateRenderingEngine engine;

  @Inject
  public TemplateRenderer(GroovyTemplateRenderingEngine engine) {
    this.engine = engine;
  }

  public void render(final Context context, final Template template) throws Exception {
    engine.renderTemplate(template.getId(), template.getModel())
      .then(byteBuf -> {
        String type = template.getType();
        if (type == null) {
          type = context.get(MimeTypes.class).getContentType(template.getId());
        }
        context.getResponse().contentType(type).send(byteBuf);
      });
  }
}
