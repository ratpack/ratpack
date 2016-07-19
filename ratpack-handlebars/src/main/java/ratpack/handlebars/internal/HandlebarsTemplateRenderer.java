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

package ratpack.handlebars.internal;

import com.github.jknack.handlebars.Handlebars;
import ratpack.file.MimeTypes;
import ratpack.handlebars.Template;
import ratpack.handling.Context;
import ratpack.render.RendererSupport;

import javax.inject.Inject;
import java.io.IOException;

public class HandlebarsTemplateRenderer extends RendererSupport<Template> {

  private final Handlebars handlebars;

  @Inject
  public HandlebarsTemplateRenderer(Handlebars handlebars) {
    this.handlebars = handlebars;
  }

  @Override
  public void render(Context ctx, Template template) {
    String contentType = template.getContentType();
    String templateName = template.getName();
    contentType = contentType == null ? ctx.get(MimeTypes.class).getContentType(templateName) : contentType;
    String renderedTemplate;
    try {
      com.github.jknack.handlebars.Template compiledTemplate = handlebars.compile(templateName);
      Object templateModel = template.getModel();
      renderedTemplate = compiledTemplate.apply(templateModel);
    } catch (IOException e) {
      ctx.error(e);
      return;
    }

    ctx.getResponse().send(contentType, renderedTemplate);
  }

  @Override
  public String toString() {
    return "HandlebarsTemplateRenderer";
  }
}
