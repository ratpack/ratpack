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

package org.ratpackframework.handlebars.internal;

import com.github.jknack.handlebars.Handlebars;
import org.ratpackframework.api.TypeLiteral;
import org.ratpackframework.file.MimeTypes;
import org.ratpackframework.handlebars.Template;
import org.ratpackframework.handling.Context;
import org.ratpackframework.render.ByTypeRenderer;

import javax.inject.Inject;
import java.io.IOException;

public class HandlebarsTemplateRenderer extends ByTypeRenderer<Template<?>> {

  private final Handlebars handlebars;

  @Inject
  public HandlebarsTemplateRenderer(Handlebars handlebars) {
    super(new TypeLiteral<Template<?>>() {});
    this.handlebars = handlebars;
  }

  @Override
  public void render(Context context, Template<?> template) {
    String contentType = template.getContentType();
    contentType = contentType == null ? context.get(MimeTypes.class).getContentType(template.getName()) : contentType;
    try {
      context.getResponse().send(contentType, handlebars.compile(template.getName()).apply(template.getModel()));
    } catch (IOException e) {
      context.error(e);
    }
  }
}
