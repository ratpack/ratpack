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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import ratpack.file.MimeTypes;
import ratpack.handlebars.Template;
import ratpack.handling.Context;
import ratpack.launch.LaunchConfig;
import ratpack.render.RendererSupport;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class HandlebarsTemplateRenderer extends RendererSupport<Template<?>> {

  private final Handlebars handlebars;

  private final LoadingCache<String, com.github.jknack.handlebars.Template> templateCache;

  @Inject
  public HandlebarsTemplateRenderer(Handlebars handlebars) {
    this.handlebars = handlebars;

    this.templateCache = CacheBuilder.newBuilder()
      .expireAfterAccess(5, TimeUnit.MINUTES)
      .build(new CacheLoader<String, com.github.jknack.handlebars.Template>() {
        @Override
        public com.github.jknack.handlebars.Template load(String key) throws Exception {
          return HandlebarsTemplateRenderer.this.handlebars.compile(key);
        }
      });
  }

  private com.github.jknack.handlebars.Template getTemplate(String templateName, Context context) throws IOException {
    if(context.get(LaunchConfig.class).isReloadable()) {
      return handlebars.compile(templateName);
    } else {
      try {
        return templateCache.get(templateName);
      } catch (ExecutionException e) {
        throw new IOException("Unable to load cached template: " + templateName, e);
      }
    }
  }

  @Override
  public void render(Context context, Template<?> template) {
    String contentType = template.getContentType();
    contentType = contentType == null ? context.get(MimeTypes.class).getContentType(template.getName()) : contentType;
    try {
      //context.getResponse().send(contentType, handlebars.compile(template.getName()).apply(template.getModel()));
      context.getResponse().send(contentType, getTemplate(template.getName(), context).apply(template.getModel()));
    } catch (IOException e) {
      context.error(e);
    }
  }
}
