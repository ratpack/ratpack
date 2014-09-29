package ratpack.site

import com.fasterxml.jackson.databind.ObjectReader
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.TypeLiteral
import groovy.util.logging.Slf4j
import ratpack.file.FileSystemChecksumServices
import ratpack.groovy.markuptemplates.MarkupTemplate
import ratpack.http.client.HttpClient
import ratpack.launch.LaunchConfig
import ratpack.render.Renderer
import ratpack.site.github.ApiBackedGitHubData
import ratpack.site.github.GitHubApi
import ratpack.site.github.GitHubData
import ratpack.site.github.RatpackVersions

@SuppressWarnings(["GrMethodMayBeStatic", "GroovyUnusedDeclaration"])
class SiteModule extends AbstractModule {

  public static final String GITHUB_ENABLE = "github.enabled"
  public static final String GITHUB_AUTH = "github.auth"
  public static final String GITHUB_TTL = "github.ttl"
  public static final String GITHUB_TTL_DEFAULT = "5"
  public static final String GITHUB_URL = "github.url"
  public static final String GITHUB_URL_DEFAULT = "https://api.github.com/"

  private final LaunchConfig launchConfig

  SiteModule(LaunchConfig launchConfig) {
    this.launchConfig = launchConfig
  }

  @Override
  protected void configure() {
//    bind(ClientErrorHandler).toInstance(new SiteErrorHandler())
//    bind(ServerErrorHandler).toInstance(new SiteErrorHandler())

    boolean enableGithub = launchConfig.getOther(GITHUB_ENABLE, "true").toBoolean()
    if (enableGithub) {
      install(new ApiModule())
      bind(RatpackVersions)
      bind(GitHubData).to(ApiBackedGitHubData)
    }

    bind(new TypeLiteral<Renderer<MarkupTemplate>>() {}).to(TemplateDecoratingRenderer.class).in(com.google.inject.Singleton.class);
  }

  @Provides
  @com.google.inject.Singleton
  AssetLinkService assetLinkService() {
    new AssetLinkService(FileSystemChecksumServices.service(launchConfig))
  }

  @Slf4j
  static class ApiModule extends AbstractModule {

    @Override
    protected void configure() {

    }

    @Provides
    @com.google.inject.Singleton
    GitHubApi gitHubApi(LaunchConfig launchConfig, ObjectReader reader, HttpClient httpClient) {
      String authToken = launchConfig.getOther(GITHUB_AUTH, null)
      if (authToken == null) {
        log.warn "Using anonymous requests to GitHub, may be rate limited (set github.auth other property)"
      }

      String ttlMins = launchConfig.getOther(GITHUB_TTL, GITHUB_TTL_DEFAULT)
      def ttlMinsInt = Integer.parseInt(ttlMins)

      String url = launchConfig.getOther(GITHUB_URL, GITHUB_URL_DEFAULT)

      new GitHubApi(url, authToken, ttlMinsInt, reader, httpClient)
    }
  }

}
