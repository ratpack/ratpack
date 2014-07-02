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
package ratpack.groovy.markuptemplates.internal;

import groovy.lang.Writable;
import groovy.text.Template;
import groovy.text.markup.MarkupTemplateEngine;
import ratpack.file.MimeTypes;
import ratpack.groovy.markuptemplates.MarkupTemplate;
import ratpack.handling.Context;
import ratpack.render.RendererSupport;

import javax.inject.Inject;
import java.io.IOException;

public class MarkupTemplateRenderer extends RendererSupport<MarkupTemplate> {

  private final MarkupTemplateEngine engine;

  @Inject
  public MarkupTemplateRenderer(MarkupTemplateEngine engine) {
    this.engine = engine;
  }

  @Override
  public void render(Context context, MarkupTemplate template) throws Exception {
    String contentType = template.getContentType();
    contentType = contentType == null ? context.get(MimeTypes.class).getContentType(template.getName()) : contentType;
    try {

      Template compiledTemplate = engine.createTemplateByPath(template.getName());
      Writable boundTemplate = compiledTemplate.make(template.getModel());
      context.getResponse().send(contentType, boundTemplate.toString());
    } catch (IOException e) {
      context.error(e);
    }
  }
}
