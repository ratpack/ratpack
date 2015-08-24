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

package ratpack.thymeleaf;

import com.google.common.base.Strings;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.cache.ICacheManager;
import org.thymeleaf.cache.StandardCacheManager;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.resourceresolver.IResourceResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolver;
import ratpack.guice.ConfigurableModule;
import ratpack.server.ServerConfig;
import ratpack.thymeleaf.internal.FileSystemBindingThymeleafResourceResolver;
import ratpack.thymeleaf.internal.ThymeleafTemplateRenderer;

import java.io.File;
import java.util.Set;

/**
 * An extension module that provides support for Thymeleaf templating engine.
 * <p>
 * To use it one has to register the module and then render {@link ratpack.thymeleaf.Template} instances.
 * Instances of {@code Template} can be created using one of the
 * {@link ratpack.thymeleaf.Template#thymeleafTemplate(java.util.Map, String, String)}
 * static methods.
 * <p>
 * By default templates are looked up in the {@code thymeleaf} directory of the application root with a {@code .html} suffix.
 * So {@code thymeleafTemplate("my/template/path")} maps to {@code thymeleaf/my/template/path.html} in the application root directory.
 * This can be configured using {@link #setTemplatesPrefix(String)} and {@link #setTemplatesSuffix(String)} as well as configuration of
 * {@link Config#templatesPrefix(String)} and {@link Config#templateSuffix(String)}.
 * <p>
 * Response content type can be manually specified, i.e. {@code thymeleafTemplate("template", model, "text/html")} if
 * not specified will default to {@code text/html}.
 * <pre class="java">{@code
 * import ratpack.guice.Guice;
 * import ratpack.test.embed.EphemeralBaseDir;
 * import ratpack.test.embed.EmbeddedApp;
 * import ratpack.thymeleaf.ThymeleafModule;
 *
 * import java.nio.file.Path;
 *
 * import static ratpack.thymeleaf.Template.thymeleafTemplate;
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *
 *   public static void main(String... args) throws Exception {
 *     EphemeralBaseDir.tmpDir().use(baseDir -> {
 *       baseDir.write("thymeleaf/myTemplate.html", "<span th:text=\"${key}\"/>");
 *       EmbeddedApp.of(s -> s
 *         .serverConfig(c -> c.baseDir(baseDir.getRoot()))
 *         .registry(Guice.registry(b -> b.module(ThymeleafModule.class)))
 *         .handlers(chain -> chain
 *           .get(ctx -> ctx.render(thymeleafTemplate("myTemplate", m -> m.put("key", "Hello Ratpack!"))))
 *         )
 *       ).test(httpClient -> {
 *         assertEquals("<span>Hello Ratpack!</span>", httpClient.getText());
 *       });
 *     });
 *   }
 * }
 * }</pre>
 * <p>
 * To register dialects, use Guice Multibindings to bind an implementation of {@code IDialect} in a module.
 *
 * @see <a href="http://www.thymeleaf.org/" target="_blank">Thymeleaf</a>
 * @see <a href="https://code.google.com/p/google-guice/wiki/Multibindings" target="_blank">Guice Multibindings</a>
 */
@SuppressWarnings("UnusedDeclaration")
public class ThymeleafModule extends ConfigurableModule<ThymeleafModule.Config> {

  /**
   * The configuration object for {@link ThymeleafModule}.
   */
  public static class Config {
    private int templatesCacheSize;
    private String templatesMode = DEFAULT_TEMPLATE_MODE;
    private String templatesPrefix = DEFAULT_TEMPLATE_PREFIX;
    private String templatesSuffix = DEFAULT_TEMPLATE_SUFFIX;

    /**
     * The size of the templates cache.
     * <p>
     * {@code 0} by default (disabled).
     *
     * @return the size of the templates cache.
     */
    public int getTemplatesCacheSize() {
      return templatesCacheSize;
    }

    /**
     * The mode for templates.
     * <p>
     * {@value ratpack.thymeleaf.ThymeleafModule#DEFAULT_TEMPLATE_MODE} by default.
     *
     * @return the mode for templates.
     */
    public String getTemplatesMode() {
      return templatesMode;
    }

    /**
     * The prefix for templates.
     * <p>
     * {@value ratpack.thymeleaf.ThymeleafModule#DEFAULT_TEMPLATE_PREFIX} by default.
     *
     * @return the prefix for templates.
     */
    public String getTemplatesPrefix() {
      return templatesPrefix;
    }

    /**
     * The suffix for templates.
     * <p>
     * {@value ratpack.thymeleaf.ThymeleafModule#DEFAULT_TEMPLATE_SUFFIX} by default.
     *
     * @return the suffix for templates.
     */
    public String getTemplatesSuffix() {
      return templatesSuffix;
    }

    /**
     * Sets the size of the templates cache.
     *
     * @param templatesCacheSize the size of the templates cache
     * @return this
     */
    public Config templatesCacheSize(int templatesCacheSize) {
      this.templatesCacheSize = templatesCacheSize;
      return this;
    }

    /**
     * Sets the mode for templates.
     *
     * @param templatesMode the mode for templates
     * @return this
     */
    public Config templatesMode(String templatesMode) {
      this.templatesMode = templatesMode;
      return this;
    }

    /**
     * Sets the prefix for templates.
     *
     * @param templatesPrefix the prefix for templates
     * @return this
     */
    public Config templatesPrefix(String templatesPrefix) {
      this.templatesPrefix = templatesPrefix;
      return this;
    }

    /**
     * Sets the suffix for templates.
     *
     * @param templatesSuffix the suffix for templates
     * @return this
     */
    public Config templateSuffix(String templatesSuffix) {
      this.templatesSuffix = templatesSuffix;
      return this;
    }
  }

  public static final String DEFAULT_TEMPLATE_MODE = "XHTML";
  public static final String DEFAULT_TEMPLATE_PREFIX = "thymeleaf";
  public static final String DEFAULT_TEMPLATE_SUFFIX = ".html";

  private String templatesMode;
  private String templatesPrefix;
  private String templatesSuffix;
  private Integer templatesCacheSize;

  public String getTemplatesMode() {
    return templatesMode;
  }

  public void setTemplatesMode(String templatesMode) {
    this.templatesMode = templatesMode;
  }

  public String getTemplatesPrefix() {
    return templatesPrefix;
  }

  public void setTemplatesPrefix(String templatesPrefix) {
    this.templatesPrefix = templatesPrefix;
  }

  public String getTemplatesSuffix() {
    return templatesSuffix;
  }

  public void setTemplatesSuffix(String templatesSuffix) {
    this.templatesSuffix = templatesSuffix;
  }

  public Integer getTemplatesCacheSize() {
    return templatesCacheSize;
  }

  public void setTemplatesCacheSize(Integer templatesCacheSize) {
    this.templatesCacheSize = templatesCacheSize;
  }

  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), IDialect.class);
    bind(ThymeleafTemplateRenderer.class).in(Singleton.class);
    bind(ICacheManager.class).to(StandardCacheManager.class).in(Singleton.class);
  }

  @Provides
  @Singleton
  ITemplateResolver provideTemplateResolver(ServerConfig serverConfig, Config config) {
    IResourceResolver resourceResolver = new FileSystemBindingThymeleafResourceResolver(serverConfig.getBaseDir());
    TemplateResolver templateResolver = new TemplateResolver();
    templateResolver.setResourceResolver(resourceResolver);
    templateResolver.setTemplateMode(getTemplatesModeSetting(config));
    templateResolver.setPrefix(getTemplatesPrefixSetting(config));
    templateResolver.setSuffix(getTemplatesSuffixSetting(config));
    templateResolver.setCacheable(getCacheSizeSetting(config) > 0);
    templateResolver.setCacheTTLMs(null); // Never use TTL expiration
    return templateResolver;
  }

  @Provides
  @Singleton
  StandardCacheManager provideCacheManager(Config config) {
    int cacheSize = getCacheSizeSetting(config);
    StandardCacheManager cacheManager = new StandardCacheManager();
    cacheManager.setTemplateCacheMaxSize(cacheSize);
    return cacheManager;
  }

  @Provides
  @Singleton
  TemplateEngine provideTemplateEngine(ITemplateResolver templateResolver, ICacheManager cacheManager, Set<IDialect> dialects) {
    final TemplateEngine templateEngine = new TemplateEngine();
    templateEngine.setTemplateResolver(templateResolver);
    templateEngine.setCacheManager(cacheManager);
    dialects.stream().forEach(templateEngine::addDialect);
    return templateEngine;
  }

  private int getCacheSizeSetting(Config config) {
    return templatesCacheSize == null ? config.getTemplatesCacheSize() : templatesCacheSize;
  }

  private String getTemplatesModeSetting(Config config) {
    return templatesMode == null ? config.getTemplatesMode() : templatesMode;
  }

  private String getTemplatesPrefixSetting(Config config) {
    String prefix = templatesPrefix == null ? config.getTemplatesPrefix() : templatesPrefix;
    if (!prefix.endsWith(File.separator)) {
      prefix += File.separator;
    }
    return prefix;
  }

  private String getTemplatesSuffixSetting(Config config) {
    String suffix = Strings.emptyToNull(templatesSuffix == null ? config.getTemplatesSuffix() : templatesSuffix);
    if (suffix == null) {
      suffix = DEFAULT_TEMPLATE_SUFFIX;
    }
    return suffix;
  }
}
