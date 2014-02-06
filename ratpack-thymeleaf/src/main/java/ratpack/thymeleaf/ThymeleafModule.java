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

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.cache.ICacheManager;
import org.thymeleaf.cache.StandardCacheManager;
import org.thymeleaf.resourceresolver.IResourceResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolver;
import ratpack.launch.LaunchConfig;
import ratpack.thymeleaf.internal.FileSystemBindingThymeleafResourceResolver;
import ratpack.thymeleaf.internal.ThymeleafTemplateRenderer;

import java.io.File;

/**
 * An extension module that provides support for Thymeleaf templating engine.
 * <p>
 * To use it one has to register the module and then render {@link ratpack.thymeleaf.Template} instances.
 * Instances of {@code Template} can be created using one of the
 * {@link ratpack.thymeleaf.Template#thymeleafTemplate(java.util.Map, String, String)}
 * static methods.
 * </p>
 * <p>
 * By default templates are looked up in the {@code thymeleaf} directory of the application root with a {@code .html} suffix.
 * So {@code thymeleafTemplate("my/template/path")} maps to {@code thymeleaf/my/template/path.html} in the application root directory.
 * This can be configured using {@link #setTemplatesPrefix(String)} and {@link #setTemplatesSuffix(String)} as well as
 * {@code other.thymeleaf.templatesPrefix} and {@code other.thymeleaf.templatesSuffix} configuration properties.
 * </p>
 * <p>
 * Response content type can be manually specified, i.e. {@code thymeleafTemplate("template", model, "text/html")} if
 * not specified will default to {@code text/html}.
 * </p>
 *
 * Example usage: (Java DSL)
 * <pre class="tested">
 * import ratpack.handling.*;
 * import ratpack.guice.*;
 * import ratpack.func.Action;
 * import ratpack.launch.*;
 * import ratpack.thymeleaf.ThymeleafModule;
 * import static ratpack.thymeleaf.Template.thymeleafTemplate;
 *
 * class MyHandler implements Handler {
 *   void handle(final Context context) {
 *     context.render(thymeleafTemplate("my/template/path", key: "it works!"));
 *   }
 * }
 *
 * class ModuleBootstrap implements Action&lt;ModuleRegistry&gt; {
 *   public void execute(ModuleRegistry modules) {
 *     modules.register(new ThymeleafModule());
 *   }
 * }
 *
 * LaunchConfig launchConfig = LaunchConfigBuilder.baseDir(new File("appRoot"))
 *     .build(new HandlerFactory() {
 *   public Handler create(LaunchConfig launchConfig) {
 *     return Guice.handler(launchConfig, new ModuleBootstrap(), new Action&lt;Chain&gt;() {
 *       public void execute(Chain chain) {
 *         chain.handler(chain.getRegistry().get(MyHandler.class));
 *       }
 *     });
 *   }
 * });
 * </pre>
 *
 * Example usage: (Groovy DSL)
 * <pre class="groovy-ratpack-dsl">
 * import ratpack.thymeleaf.ThymeleafModule
 * import static ratpack.thymeleaf.Template.thymeleafTemplate
 * import static ratpack.groovy.Groovy.ratpack
 *
 * ratpack {
 *   modules {
 *     register new ThymeleafModule()
 *   }
 *   handlers {
 *     get {
 *       render thymeleafTemplate('my/template/path', key: 'it works!')
 *     }
 *   }
 * }
 * </pre>
 *
 * @see <a href="http://www.thymeleaf.org/" target="_blank">Thymeleaf</a>
 */
public class ThymeleafModule extends AbstractModule {

  private static final String DEFAULT_TEMPLATE_MODE = "XHTML";
  private static final String DEFAULT_TEMPLATE_PREFIX = "thymeleaf";
  private static final String DEFAULT_TEMPLATE_SUFFIX = ".html";

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
    bind(ThymeleafTemplateRenderer.class).in(Singleton.class);
    bind(ICacheManager.class).to(StandardCacheManager.class).in(Singleton.class);
  }

  @SuppressWarnings("UnusedDeclaration")
  @Provides
  @Singleton
  ITemplateResolver provideTemplateResolver(LaunchConfig launchConfig) {
    IResourceResolver resourceResolver = new FileSystemBindingThymeleafResourceResolver(launchConfig.getBaseDir());
    TemplateResolver templateResolver = new TemplateResolver();
    templateResolver.setResourceResolver(resourceResolver);


    String mode = templatesMode == null ? launchConfig.getOther("thymeleaf.templatesMode", DEFAULT_TEMPLATE_MODE) : templatesMode;
    templateResolver.setTemplateMode(mode);

    String prefix = templatesPrefix == null ? launchConfig.getOther("thymeleaf.templatesPrefix", DEFAULT_TEMPLATE_PREFIX) : templatesPrefix;
    if (!prefix.endsWith(File.separator)) {
      prefix += File.separator;
    }
    templateResolver.setPrefix(prefix);

    String suffix = templatesSuffix == null ? launchConfig.getOther("thymeleaf.templatesSuffix", DEFAULT_TEMPLATE_SUFFIX) : templatesSuffix;
    if (suffix.equalsIgnoreCase("")) {
      suffix = DEFAULT_TEMPLATE_SUFFIX;
    }
    templateResolver.setSuffix(suffix);

    Integer cacheSize = getCacheSizeSetting(launchConfig);
    templateResolver.setCacheable(cacheSize > 0);

    // Never use TTL expiration
    templateResolver.setCacheTTLMs(null);

    return templateResolver;
  }

  @SuppressWarnings("UnusedDeclaration")
  @Provides
  @Singleton
  StandardCacheManager provideCacheManager(LaunchConfig launchConfig) {
    Integer cacheSize = getCacheSizeSetting(launchConfig);
    StandardCacheManager cacheManager = new StandardCacheManager();
    cacheManager.setTemplateCacheMaxSize(cacheSize);
    return cacheManager;
  }

  @SuppressWarnings("UnusedDeclaration")
  @Provides
  @Singleton
  TemplateEngine provideTemplateEngine(ITemplateResolver templateResolver, ICacheManager cacheManager) {
    final TemplateEngine templateEngine = new TemplateEngine();
    templateEngine.setTemplateResolver(templateResolver);
    templateEngine.setCacheManager(cacheManager);
    return templateEngine;
  }

  private Integer getCacheSizeSetting(LaunchConfig launchConfig) {
    return templatesCacheSize == null ? Integer.valueOf(launchConfig.getOther("thymeleaf.templatesCacheSize", "0")) : templatesCacheSize;
  }
}
