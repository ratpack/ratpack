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

/**
 * An extension module that provides support for Handlebars.java templating engine.
 * <p>
 * To use it one has to register the module and then render {@link org.ratpackframework.handlebars.Template} instances.
 * Instances of {@code Template} can be created using one of the
 * {@link org.ratpackframework.handlebars.Template#handlebarsTemplate(java.util.Map, String, String)}
 * static methods.
 * </p>
 * <p>
 * By default templates are looked up in the {@code handlebars} directory of the application root with a {@code .hbs} suffix.
 * So {@code handlebarsTemplate("my/template/path")} maps to {@code handlebars/my/template/path.hbs} in the application root directory.
 * This can be configured using {@link #setTemplatesPath(String)} and {@link #setTemplatesSuffix(String)} as well as
 * {@code other.handlebars.templatesPath} and {@code other.handlebars.templatesSuffix} configuration properties.
 * </p>
 * <p>
 * Response content type is determined based on the extension of the template file - i.e. for {@code handlebarsTemplate("template.html")}
 * it would be {@code text/html}. It can also be also manually specified if required, i.e. {@code handlebarsTemplate("template", model, "text/html")}
 * </p>
 * <p>Custom handlebars helpers can be registered by binding instances of {@link org.ratpackframework.handlebars.NamedHelper}.</p>
 *
 * Example usage: (Java DSL)
 * <pre class="tested">
 * import org.ratpackframework.handling.*;
 * import org.ratpackframework.guice.*;
 * import org.ratpackframework.util.*;
 * import org.ratpackframework.launch.*;
 * import org.ratpackframework.handlebars.HandlebarsModule;
 * import static org.ratpackframework.handlebars.Template.handlebarsTemplate;
 *
 * class MyHandler implements Handler {
 *   void handle(final Context context) {
 *     context.render(handlebarsTemplate("my/template/path", key: "it works!"));
 *   }
 * }
 *
 * class ModuleBootstrap implements Action&lt;ModuleRegistry&gt; {
 *   public void execute(ModuleRegistry modules) {
 *     modules.register(new HandlebarsModule());
 *   }
 * }
 *
 * LaunchConfig launchConfig = LaunchConfigBuilder.baseDir(new File("appRoot"))
 *   .build(new HandlerFactory() {
 *     public Handler create(LaunchConfig launchConfig) {
 *       return Guice.handler(launchConfig, new ModuleBootstrap(), new Action&lt;Chain&gt;() {
 *         public void execute(Chain chain) {
 *           chain.add(chain.getRegistry().get(MyHandler.class));
 *         }
 *       });
 *     }
 *   });
 * </pre>
 *
 * Example usage: (Groovy DSL)
 * <pre class="groovy-ratpack-dsl">
 * import org.ratpackframework.handlebars.HandlebarsModule
 * import static org.ratpackframework.handlebars.Template.handlebarsTemplate
 *
 * modules {
 *   register new HandlebarsModule()
 * }
 * handlers {
 *   get {
 *     render handlebarsTemplate('my/template/path', key: 'it works!')
 *   }
 * }
 * </pre>
 *
 * @see <a href="http://jknack.github.io/handlebars.java/" target="_blank">Handlebars.java</a>
 */
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
