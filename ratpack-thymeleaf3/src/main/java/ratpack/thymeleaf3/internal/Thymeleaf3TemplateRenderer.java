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

package ratpack.thymeleaf3.internal;

import org.thymeleaf.TemplateEngine;
import ratpack.handling.Context;
import ratpack.render.RendererSupport;
import ratpack.thymeleaf3.Template;

import javax.inject.Inject;

public final class Thymeleaf3TemplateRenderer extends RendererSupport<Template> {

  private final TemplateEngine templateEngine;

  @Inject
  public Thymeleaf3TemplateRenderer(TemplateEngine templateEngine) {
    this.templateEngine = templateEngine;
  }

  @Override
  public void render(Context ctx, Template template) throws Exception {
    try {
      ctx.getResponse().contentTypeIfNotSet("text/html");
      String processed = templateEngine.process(template.getName(), template.getContext());
      ctx.getResponse().send(processed);
    } catch (Exception e) {
      ctx.error(e);
    }
  }

}
