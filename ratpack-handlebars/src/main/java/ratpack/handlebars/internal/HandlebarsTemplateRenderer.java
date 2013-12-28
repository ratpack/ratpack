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
import ratpack.util.Action;

import javax.inject.Inject;
import java.util.concurrent.Callable;

public class HandlebarsTemplateRenderer extends RendererSupport<Template<?>> {

  private final Handlebars handlebars;

  @Inject
  public HandlebarsTemplateRenderer(Handlebars handlebars) {
    this.handlebars = handlebars;
  }

  @Override
  public void render(final Context context, final Template<?> template) {
    String requestedContentType = template.getContentType();
    final String contentType = requestedContentType == null ? context.get(MimeTypes.class).getContentType(template.getName()) : requestedContentType;
    context.getBackground().exec(new Callable<com.github.jknack.handlebars.Template>() {
      public com.github.jknack.handlebars.Template call() throws Exception {
        return handlebars.compile(template.getName());
      }
    }).then(new Action<com.github.jknack.handlebars.Template>() {
      public void execute(com.github.jknack.handlebars.Template compiledTemplate) throws Exception {
        context.getResponse().send(contentType, compiledTemplate.apply(template.getModel()));
      }
    });
  }
}
