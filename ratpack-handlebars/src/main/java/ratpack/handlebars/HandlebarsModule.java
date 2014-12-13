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
import com.github.jknack.handlebars.cache.TemplateCache;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.google.common.cache.CacheBuilder;
import com.google.common.reflect.TypeToken;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import ratpack.file.FileSystemBinding;
import ratpack.guice.internal.GuiceUtil;
import ratpack.handlebars.internal.FileSystemBindingTemplateLoader;
import ratpack.handlebars.internal.HandlebarsTemplateRenderer;
import ratpack.handlebars.internal.RatpackTemplateCache;
import ratpack.handlebars.internal.TemplateKey;
import ratpack.launch.ServerConfig;

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
 * This can be configured using {@link #setTemplatesPath(String)} and {@link #setTemplatesSuffix(String)} as well as
 * {@code other.handlebars.templatesPath} and {@code other.handlebars.templatesSuffix} configuration properties.
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
 * import ratpack.test.embed.BaseDirBuilder;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import java.nio.file.Path;
 *
 * import static ratpack.handlebars.Template.handlebarsTemplate;
 *
 * public class Example {
 *
 *   public static void main(String... args) {
 *     Path baseDir = BaseDirBuilder.tmpDir().build(builder ->
 *         builder.file("handlebars/myTemplate.html.hbs", "Hello {{name}}!")
 *     );
 *     EmbeddedApp.fromHandlerFactory(baseDir, launchConfig ->
 *         Guice.builder(launchConfig)
 *           .bindings(b -> b.add(new HandlebarsModule()))
 *           .build(chain -> chain
 *               .get(ctx -> ctx.render(handlebarsTemplate("myTemplate.html", m -> m.put("name", "Ratpack"))))
 *           )
 *     ).test(httpClient -> {
 *       assert httpClient.getText().equals("Hello Ratpack!");
 *     });
 *   }
 * }
 * }</pre>
 *
 * @see <a href="http://jknack.github.io/handlebars.java/" target="_blank">Handlebars.java</a>
 */
public class HandlebarsModule extends AbstractModule {

  private static final TypeToken<NamedHelper<?>> NAMED_HELPER_TYPE = new TypeToken<NamedHelper<?>>() {
  };

  private String templatesPath;

  private String templatesSuffix;

  private Integer cacheSize;

  private Boolean reloadable;

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

  public int getCacheSize() {
    return cacheSize;
  }

  public void setCacheSize(int cacheSize) {
    this.cacheSize = cacheSize;
  }

  public boolean isReloadable() {
    return reloadable;
  }

  public void setReloadable(boolean reloadable) {
    this.reloadable = reloadable;
  }

  @Override
  protected void configure() {
    bind(HandlebarsTemplateRenderer.class).in(Singleton.class);
  }

  @SuppressWarnings("UnusedDeclaration")
  @Provides
  @Singleton
  TemplateLoader provideTemplateLoader(ServerConfig serverConfig) {
    String path = templatesPath == null ? serverConfig.getOther("handlebars.templatesPath", "handlebars") : templatesPath;
    String suffix = templatesSuffix == null ? serverConfig.getOther("handlebars.templatesSuffix", ".hbs") : templatesSuffix;

    FileSystemBinding templatesBinding = serverConfig.getBaseDir().binding(path);
    return new FileSystemBindingTemplateLoader(templatesBinding, suffix);
  }

  @SuppressWarnings("UnusedDeclaration")
  @Provides
  TemplateCache provideTemplateCache(ServerConfig serverConfig) {
    boolean reloadable = this.reloadable == null ? serverConfig.isDevelopment() : this.reloadable;
    int cacheSize = this.cacheSize == null ? Integer.parseInt(serverConfig.getOther("handlebars.cacheSize", "100")) : this.cacheSize;
    return new RatpackTemplateCache(reloadable, CacheBuilder.newBuilder().maximumSize(cacheSize).<TemplateKey, com.github.jknack.handlebars.Template>build());
  }

  @SuppressWarnings("UnusedDeclaration")
  @Provides
  @Singleton
  Handlebars provideHandlebars(Injector injector, TemplateLoader templateLoader, TemplateCache templateCache) {

    final Handlebars handlebars = new Handlebars().with(templateLoader);
    handlebars.with(templateCache);
    GuiceUtil.eachOfType(injector, NAMED_HELPER_TYPE, helper -> handlebars.registerHelper(helper.getName(), helper));

    return handlebars;
  }
}
