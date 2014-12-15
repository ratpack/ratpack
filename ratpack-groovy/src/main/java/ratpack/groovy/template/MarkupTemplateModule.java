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
package ratpack.groovy.template;

import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;
import ratpack.groovy.template.internal.CachingTemplateResolver;
import ratpack.groovy.template.internal.MarkupTemplateRenderer;
import ratpack.guice.ConfigurableModule;
import ratpack.launch.LaunchConfig;
import ratpack.render.Renderer;

import javax.inject.Singleton;
import java.nio.file.Path;

/**
 * An extension module that provides support for the Groovy markup template engine.
 * <p>
 * To use it one has to register the module and then render {@link MarkupTemplate} instances.
 * Instances of {@link MarkupTemplate} can be created using one of the
 * {@link ratpack.groovy.Groovy#groovyMarkupTemplate(java.util.Map, String, String)}
 * static methods.
 * </p>
 * <p>
 * By default templates are looked up in the {@code templates} directory of the application root.
 * So {@code groovyMarkupTemplate("my/template/path.gtpl")} maps to {@code tempaltes/my/template/path.gtpl} in the application root directory.
 * <p>
 * The template engine can be configured using the {@link TemplateConfiguration template configuration}. In particular, it is possible to configure
 * things like automatic indentation.
 * </p>
 * <p>
 * Response content type can be manually specified, i.e. {@code groovyMarkupTemplate("template.gtpl", model, "text/html")} if
 * not specified will default to {@code text/html}.
 * <pre class="java">{@code
 * import ratpack.groovy.template.MarkupTemplateModule;
 * import ratpack.guice.Guice;
 * import ratpack.test.embed.BaseDirBuilder;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import java.nio.file.Path;
 *
 * import static ratpack.groovy.Groovy.groovyMarkupTemplate;
 *
 * public class Example {
 *
 *   public static void main(String... args) {
 *     Path baseDir = BaseDirBuilder.tmpDir().build(builder ->
 *         builder.file("templates/myTemplate.gtpl", "html { body { p(value) } }")
 *     );
 *     EmbeddedApp.fromHandlerFactory(baseDir, launchConfig ->
 *         Guice.builder(launchConfig)
 *           .bindings(b -> b.add(MarkupTemplateModule.class))
 *           .build(chain -> chain
 *               .get(ctx -> ctx.render(groovyMarkupTemplate("myTemplate.gtpl", m -> m.put("value", "hello!"))))
 *           )
 *     ).test(httpClient -> {
 *       assert httpClient.get().getBody().getText().equals("<html><body><p>hello!</p></body></html>");
 *     });
 *   }
 * }
 * }</pre>
 *
 * @see <a href="http://beta.groovy-lang.org/docs/latest/html/documentation/markup-template-engine.html" target="_blank">Groovy Markup Template Engine</a>
 */
public class MarkupTemplateModule extends ConfigurableModule<MarkupTemplateModule.Config> {

  @Override
  protected void configure() {
    bind(new TypeLiteral<Renderer<MarkupTemplate>>() {
    }).to(MarkupTemplateRenderer.class).in(Singleton.class);
  }

  @Override
  protected void defaultConfig(LaunchConfig launchConfig, Config config) {
    config.setAutoEscape(true);
    config.setCacheTemplates(!launchConfig.isDevelopment());
  }

  @SuppressWarnings("UnusedDeclaration")
  @Provides
  @Singleton
  MarkupTemplateEngine provideTemplateEngine(LaunchConfig launchConfig, Config config) {
    ClassLoader parent = getClass().getClassLoader();
    TemplateConfiguration effectiveConfiguration = new TemplateConfiguration(config);
    effectiveConfiguration.setCacheTemplates(config.isCacheTemplates()); // not copied by constructor
    Path templatesDir = launchConfig.getBaseDir().file(config.getTemplatesDirectory());
    return new MarkupTemplateEngine(parent, effectiveConfiguration, new CachingTemplateResolver(templatesDir));
  }

  public static class Config extends TemplateConfiguration {

    private String templatesDirectory = "templates";

    public String getTemplatesDirectory() {
      return templatesDirectory;
    }

    public void setTemplatesDirectory(String templatesDirectory) {
      this.templatesDirectory = templatesDirectory;
    }

    public Config() {
    }

  }
}
