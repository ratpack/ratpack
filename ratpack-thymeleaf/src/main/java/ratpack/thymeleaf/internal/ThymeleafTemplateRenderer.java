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

package ratpack.thymeleaf.internal;

import org.thymeleaf.TemplateEngine;
import ratpack.handling.Context;
import ratpack.render.RendererSupport;
import ratpack.thymeleaf.Template;

import javax.inject.Inject;

public class ThymeleafTemplateRenderer extends RendererSupport<Template> {

  private final TemplateEngine thymeleaf;

  @Inject
  public ThymeleafTemplateRenderer(TemplateEngine thymeleaf) {
    this.thymeleaf = thymeleaf;
  }

  @Override
  public void render(Context ctx, Template template) {
    String contentType = template.getContentType();
    contentType = contentType == null ? "text/html" : contentType;
    try {
      ctx.getResponse().send(contentType, thymeleaf.process(template.getName(), template.getModel(), template.getFragmentSpec()));
    } catch (Exception e) {
      ctx.error(e);
    }
  }
}
