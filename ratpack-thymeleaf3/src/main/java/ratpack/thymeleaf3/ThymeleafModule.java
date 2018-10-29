/*
 * Copyright 2018 the original author or authors.
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

package ratpack.thymeleaf3;

import com.google.common.base.Strings;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;
import ratpack.guice.ConfigurableModule;
import ratpack.server.ServerConfig;
import ratpack.thymeleaf3.internal.FileSystemBindingThymeleaf3TemplateResolver;
import ratpack.thymeleaf3.internal.Thymeleaf3TemplateRenderer;

import java.io.File;
import java.util.Map;

/**
 * An extension module that provides support for Thymeleaf templating engine.
 * <p>
 * To use it one has to register the module and then render {@link Template} instances.
 * Instances of {@code Template} can be created using one of the
 * {@link Template#thymeleafTemplate(String, Map)}
 * static methods.
 * <p>
 * By default templates are looked up in the {@code thymeleaf} directory of the application root with a {@code .html} suffix.
 * So {@code thymeleafTemplate("my/template/path")} maps to {@code thymeleaf/my/template/path.html} in the application root directory.
 * This can be configured using {@link #setTemplatesPrefix(String)} and {@link #setTemplatesSuffix(String)} as well as configuration of
 * {@link Config#templatesPrefix(String)} and {@link Config#templateSuffix(String)}.
 * <p>
 *
 * @see <a href="http://www.thymeleaf.org/" target="_blank">Thymeleaf</a>
 */
public final class ThymeleafModule extends ConfigurableModule<ThymeleafModule.Config> {

  /**
   * The configuration object for {@link ThymeleafModule}.
   */
  public static class Config {
    private String templatesMode = DEFAULT_TEMPLATE_MODE;
    private String templatesPrefix = DEFAULT_TEMPLATE_PREFIX;
    private String templatesSuffix = DEFAULT_TEMPLATE_SUFFIX;

    /**
     * The mode for templates.
     * <p>
     * {@value ThymeleafModule#DEFAULT_TEMPLATE_MODE} by default.
     *
     * @return the mode for templates.
     */
    public String getTemplatesMode() {
      return templatesMode;
    }

    /**
     * The prefix for templates.
     * <p>
     * {@value ThymeleafModule#DEFAULT_TEMPLATE_PREFIX} by default.
     *
     * @return the prefix for templates.
     */
    public String getTemplatesPrefix() {
      return templatesPrefix;
    }

    /**
     * The suffix for templates.
     * <p>
     * {@value ThymeleafModule#DEFAULT_TEMPLATE_SUFFIX} by default.
     *
     * @return the suffix for templates.
     */
    public String getTemplatesSuffix() {
      return templatesSuffix;
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

  public static final String DEFAULT_TEMPLATE_MODE = "HTML";
  public static final String DEFAULT_TEMPLATE_PREFIX = "thymeleaf";
  public static final String DEFAULT_TEMPLATE_SUFFIX = ".html";

  private String templatesMode;
  private String templatesPrefix;
  private String templatesSuffix;

  public String getTemplatesMode() {
    return templatesMode;
  }

  public String getTemplatesPrefix() {
    return templatesPrefix;
  }

  public String getTemplatesSuffix() {
    return templatesSuffix;
  }

  public void setTemplatesMode(String templatesMode) {
    this.templatesMode = templatesMode;
  }

  public void setTemplatesPrefix(String templatesPrefix) {
    this.templatesPrefix = templatesPrefix;
  }

  public void setTemplatesSuffix(String templatesSuffix) {
    this.templatesSuffix = templatesSuffix;
  }

  @Override
  protected void configure() {
    bind(Thymeleaf3TemplateRenderer.class).in(Singleton.class);
  }

  @Provides
  @Singleton
  ITemplateResolver provideTemplateResolver(ServerConfig serverConfig, Config config) {
    AbstractConfigurableTemplateResolver resolver = new FileSystemBindingThymeleaf3TemplateResolver(serverConfig.getBaseDir());
    resolver.setTemplateMode(getTemplatesModeSetting(config));
    resolver.setPrefix(getTemplatesPrefixSetting(config));
    resolver.setSuffix(getTemplatesSuffixSetting(config));
    return resolver;
  }

  @Provides
  @Singleton
  TemplateEngine provideTemplateEngine(ITemplateResolver templateResolver) {
    TemplateEngine templateEngine = new TemplateEngine();
    templateEngine.setTemplateResolver(templateResolver);
    return templateEngine;
  }


  private String getTemplatesModeSetting(Config config) {
    return Strings.isNullOrEmpty(getTemplatesMode()) ? config.getTemplatesMode() : getTemplatesMode();
  }

  private String getTemplatesPrefixSetting(Config config) {
    String prefix = Strings.isNullOrEmpty(getTemplatesPrefix()) ? config.getTemplatesPrefix() : getTemplatesPrefix();
    return prefix.endsWith(File.separator) ? prefix : prefix + File.separator;
  }

  private String getTemplatesSuffixSetting(Config config) {
    String suffix = Strings.isNullOrEmpty(getTemplatesSuffix()) ? config.getTemplatesSuffix() : getTemplatesSuffix();
    return Strings.isNullOrEmpty(suffix) ? DEFAULT_TEMPLATE_SUFFIX : suffix;
  }

}
