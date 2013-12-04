package ratpack.thymeleaf;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;
import ratpack.launch.LaunchConfig;
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
 * So {@code thymeleafTemplate("my/template/path")} maps to {@code handlebars/my/template/path.html} in the application root directory.
 * This can be configured using {@link #setTemplatesPrefix(String)} and {@link #setTemplatesSuffix(String)} as well as
 * {@code other.thymeleaf.templatesPrefix} and {@code other.handlebars.templatesSuffix} configuration properties.
 * </p>
 * <p>
 * Response content type can be manually specified, i.e. {@code thymeleafTemplate("template", model, "text/html")} or can
 * be detected based on the template extension. Mapping between file extensions and content types is performed using
 * {@link ratpack.file.MimeTypes} contextual object so content type for {@code handlebarsTemplate("template.html")}
 * would be {@code text/html} by default.
 * </p>
 *
 * Example usage: (Java DSL)
 * <pre class="tested">
 * import ratpack.handling.*;
 * import ratpack.guice.*;
 * import ratpack.util.*;
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
  private Boolean templatesCacheable;
  private Long cacheTTLMs;

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

  public Boolean isTemplatesCacheable() {
    return templatesCacheable;
  }

  public void setTemplatesCacheable(Boolean templatesCacheable) {
    this.templatesCacheable = templatesCacheable;
  }

  public Long getCacheTTLMs() {
    return cacheTTLMs;
  }

  public void setCacheTTLMs(Long cacheTTLMs) {
    this.cacheTTLMs = cacheTTLMs;
  }

  @Override
  protected void configure() {
    bind(ThymeleafTemplateRenderer.class).in(Singleton.class);
    bind(ITemplateResolver.class).to(FileTemplateResolver.class).in(Singleton.class);
  }

  @SuppressWarnings("UnusedDeclaration")
  @Provides
  FileTemplateResolver provideITemplateResolver(LaunchConfig launchConfig) {
    final FileTemplateResolver templateResolver = new FileTemplateResolver();

    String mode = templatesMode == null ? launchConfig.getOther("thymeleaf.templatesMode", DEFAULT_TEMPLATE_MODE) : templatesMode;
    String prefix = templatesPrefix == null ? launchConfig.getOther("thymeleaf.templatesPrefix", DEFAULT_TEMPLATE_PREFIX) : templatesPrefix;
    String suffix = templatesSuffix == null ? launchConfig.getOther("thymeleaf.templatesSuffix", DEFAULT_TEMPLATE_SUFFIX) : templatesSuffix;
    Boolean cacheable = templatesCacheable == null ? Boolean.valueOf(launchConfig.getOther("thymeleaf.templatesCacheable", "true")) : templatesCacheable;
    Long cacheTTL = cacheTTLMs == null ? Long.valueOf(launchConfig.getOther("thymeleaf.cacheTTLMs", "0")) : cacheTTLMs;

    templateResolver.setTemplateMode(mode);

    File finalPrefixPathFile = new File(launchConfig.getBaseDir(), prefix);
    String path = finalPrefixPathFile.getAbsolutePath();
    if (path.charAt(path.length() - 1) != File.separatorChar) {
      path += File.separator;
    }
    templateResolver.setPrefix(path);

    if (suffix.equalsIgnoreCase("")) {
      suffix = DEFAULT_TEMPLATE_SUFFIX;
    }
    templateResolver.setSuffix(suffix);

    templateResolver.setCacheable(cacheable != null && cacheable);
    templateResolver.setCacheTTLMs(cacheTTL != null && cacheTTL > 0 ? cacheTTL : null);

    return templateResolver;
  }

  @SuppressWarnings("UnusedDeclaration")
  @Provides
  @Singleton
  TemplateEngine provideTemplateEngine(ITemplateResolver templateResolver) {
    final TemplateEngine templateEngine = new TemplateEngine();
    templateEngine.setTemplateResolver(templateResolver);
    return templateEngine;
  }
}