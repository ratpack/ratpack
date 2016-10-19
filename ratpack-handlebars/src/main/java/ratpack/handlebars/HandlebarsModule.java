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

package ratpack.handlebars;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.cache.ConcurrentMapTemplateCache;
import com.github.jknack.handlebars.cache.TemplateCache;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.google.common.reflect.TypeToken;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import ratpack.file.FileSystemBinding;
import ratpack.guice.ConfigurableModule;
import ratpack.guice.internal.GuiceUtil;
import ratpack.handlebars.internal.FileSystemBindingTemplateLoader;
import ratpack.handlebars.internal.HandlebarsTemplateRenderer;
import ratpack.server.ServerConfig;

/**
 * An extension module that provides support for Handlebars.java templating engine.
 * <p>
 * To use it one has to register the module and then render {@link ratpack.handlebars.Template} instances.
 * Instances of {@code Template} can be created using one of the
 * {@link ratpack.handlebars.Template#handlebarsTemplate(java.util.Map, String, String)}
 * static methods.
 * </p>
 * <p>
 * By default templates are looked up in the {@code handlebars} directory of the application root with a {@code .hbs} suffix.
 * So {@code handlebarsTemplate("my/template/path")} maps to {@code handlebars/my/template/path.hbs} in the application root directory.
 * This can be configured using {@link ratpack.handlebars.HandlebarsModule.Config#templatesPath(String)} and {@link ratpack.handlebars.HandlebarsModule.Config#templatesSuffix(String)}.
 * </p>
 * <p>
 * The default template delimiters are {@code {{ }}} but can be configured using {@link ratpack.handlebars.HandlebarsModule.Config#delimiters(String, String)}.
 * </p>
 * <p>
 * Response content type can be manually specified, i.e. {@code handlebarsTemplate("template", model, "text/html")} or can
 * be detected based on the template extension. Mapping between file extensions and content types is performed using
 * {@link ratpack.file.MimeTypes} contextual object so content type for {@code handlebarsTemplate("template.html")}
 * would be {@code text/html} by default.
 * </p>
 * <p>Custom handlebars helpers can be registered by binding instances of {@link ratpack.handlebars.NamedHelper}.</p>
 * <pre class="java">{@code
 * import ratpack.guice.Guice;
 * import ratpack.handlebars.HandlebarsModule;
 * import ratpack.test.embed.EphemeralBaseDir;
 * import ratpack.test.embed.EmbeddedApp;
 * import java.nio.file.Path;
 *
 * import static ratpack.handlebars.Template.handlebarsTemplate;
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *   public static void main(String... args) throws Exception {
 *     EphemeralBaseDir.tmpDir().use(baseDir -> {
 *       baseDir.write("handlebars/myTemplate.html.hbs", "Hello {{name}}!");
 *       EmbeddedApp.of(s -> s
 *         .serverConfig(c -> c.baseDir(baseDir.getRoot()))
 *         .registry(Guice.registry(b -> b.module(HandlebarsModule.class)))
 *         .handlers(chain -> chain
 *           .get(ctx -> ctx.render(handlebarsTemplate("myTemplate.html", m -> m.put("name", "Ratpack"))))
 *         )
 *       ).test(httpClient -> {
 *         assertEquals("Hello Ratpack!", httpClient.getText());
 *       });
 *     });
 *   }
 * }
 * }</pre>
 *
 * @see <a href="http://jknack.github.io/handlebars.java/" target="_blank">Handlebars.java</a>
 */
public class HandlebarsModule extends ConfigurableModule<HandlebarsModule.Config> {

  private static final TypeToken<NamedHelper<?>> NAMED_HELPER_TYPE = new TypeToken<NamedHelper<?>>() {
  };

  /**
   * The configuration object for {@link HandlebarsModule}.
   */
  public static class Config {
    private String templatesPath = "handlebars";

    private String templatesSuffix = ".hbs";

    private String startDelimiter = Handlebars.DELIM_START;

    private String endDelimiter = Handlebars.DELIM_END;

    private int cacheSize = 100;

    private Boolean reloadable;

    public String getTemplatesPath() {
      return templatesPath;
    }

    public Config templatesPath(String templatesPath) {
      this.templatesPath = templatesPath;
      return this;
    }

    public String getTemplatesSuffix() {
      return templatesSuffix;
    }

    public Config templatesSuffix(String templatesSuffix) {
      this.templatesSuffix = templatesSuffix;
      return this;
    }

    public String getStartDelimiter() {
      return startDelimiter;
    }

    public String getEndDelimiter() {
      return endDelimiter;
    }

    public Config delimiters(String startDelimiter, String endDelimiter) {
      this.startDelimiter = startDelimiter;
      this.endDelimiter = endDelimiter;
      return this;
    }

    public int getCacheSize() {
      return cacheSize;
    }

    public Config cacheSize(int cacheSize) {
      this.cacheSize = cacheSize;
      return this;
    }

    public Boolean isReloadable() {
      return reloadable;
    }

    public Config reloadable(boolean reloadable) {
      this.reloadable = reloadable;
      return this;
    }
  }

  @Override
  protected void configure() {
    bind(HandlebarsTemplateRenderer.class).in(Singleton.class);
  }

  @SuppressWarnings("UnusedDeclaration")
  @Provides
  @Singleton
  TemplateLoader provideTemplateLoader(Config config, FileSystemBinding fileSystemBinding) {
    FileSystemBinding templatesBinding = fileSystemBinding.binding(config.getTemplatesPath());
    return new FileSystemBindingTemplateLoader(templatesBinding, config.getTemplatesSuffix());
  }

  @SuppressWarnings("UnusedDeclaration")
  @Provides
  @Singleton
  TemplateCache provideTemplateCache(Config config, ServerConfig serverConfig) {
    boolean reloadable = config.isReloadable() == null ? serverConfig.isDevelopment() : config.isReloadable();
    return new ConcurrentMapTemplateCache().setReload(reloadable);
  }

  @SuppressWarnings("UnusedDeclaration")
  @Provides
  @Singleton
  Handlebars provideHandlebars(Config config, Injector injector, TemplateLoader templateLoader, TemplateCache templateCache) {
    final Handlebars handlebars = new Handlebars()
      .with(templateLoader)
      .with(templateCache)
      .startDelimiter(config.getStartDelimiter())
      .endDelimiter(config.getEndDelimiter());

    GuiceUtil.eachOfType(injector, NAMED_HELPER_TYPE, helper -> handlebars.registerHelper(helper.getName(), helper));

    return handlebars;
  }
}
