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

package org.ratpackframework.handlebars;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.google.inject.*;
import org.ratpackframework.guice.internal.GuiceUtil;
import org.ratpackframework.handlebars.internal.HandlebarsTemplateRenderer;
import org.ratpackframework.launch.LaunchConfig;
import org.ratpackframework.util.Action;

import java.io.File;

public class HandlebarsModule extends AbstractModule {

  private String templatesPath;

  private String templatesSuffix;

  public String getTemplatesPath() {
    return templatesPath;
  }

  public void setTemplatesPath(String templatesPath) {
    this.templatesPath = templatesPath;
  }

  public String getTemplatesSuffix() {
    return templatesSuffix;
  }

  public void setTemplatesSuffix(String templatesSuffix) {
    this.templatesSuffix = templatesSuffix;
  }

  @Override
  protected void configure() {
    bind(HandlebarsTemplateRenderer.class).in(Singleton.class);
    bind(TemplateLoader.class).to(FileTemplateLoader.class).in(Singleton.class);
  }

  @Provides
  FileTemplateLoader provideTemplateLoader(LaunchConfig launchConfig) {
    String path = templatesPath == null ? launchConfig.getOther("handlebars.templatesPath", "handlebars") : templatesPath;
    String suffix = templatesSuffix == null ? launchConfig.getOther("handlebars.templatesSuffix", ".hbs") : templatesSuffix;
    File templatesPathFile = new File(launchConfig.getBaseDir(), path);
    return new FileTemplateLoader(templatesPathFile, suffix);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Provides @Singleton
  Handlebars provideHandlebars(Injector injector, TemplateLoader templateLoader) {
    final Handlebars handlebars = new Handlebars().with(templateLoader);

    TypeLiteral<NamedHelper<?>> type = new TypeLiteral<NamedHelper<?>>() {
    };

    GuiceUtil.eachOfType(injector, type, new Action<NamedHelper<?>>() {
      public void execute(NamedHelper<?> helper) {
        handlebars.registerHelper(helper.getName(), helper);
      }
    });

    return handlebars;
  }
}
