package ratpack.site

import com.fasterxml.jackson.databind.ObjectReader
import com.google.inject.Provides
import groovy.util.logging.Slf4j
import ratpack.file.FileSystemChecksumServices
import ratpack.groovy.template.MarkupTemplate
import ratpack.guice.ConfigurableModule
import ratpack.http.client.HttpClient
import ratpack.render.RenderableDecorator
import ratpack.server.ServerConfig
import ratpack.site.github.*

import javax.inject.Provider
import javax.inject.Singleton

@Slf4j
@SuppressWarnings(["GrMethodMayBeStatic", "GroovyUnusedDeclaration"])
class SiteModule extends ConfigurableModule<GitHubConfig> {

  static class GitHubConfig {
    boolean enabled = true
    String auth
    Integer ttl = 5
    String url = "https://api.github.com/"
  }

  @Override
  protected void configure() {
    bind(RatpackVersions)
    bind(SiteErrorHandler)
  }

  @Provides
  @Singleton
  GitHubApi gitHubApi(GitHubConfig config, ObjectReader reader, HttpClient httpClient) {
    String authToken = config.auth
    if (authToken == null) {
      log.warn "Using anonymous requests to GitHub, may be rate limited (set github.auth property)"
    }
    def ttlMinsInt = config.ttl
    String url = config.url
    new GitHubApi(url, authToken, ttlMinsInt, reader, httpClient)
  }

  @Provides
  @Singleton
  GitHubData gitHubData(GitHubConfig config, Provider<GitHubApi> apiProvider) {
    config.enabled ? new ApiBackedGitHubData(apiProvider.get()) : new NullGitHubData()
  }

  @Provides
  @Singleton
  AssetLinkService assetLinkService(ServerConfig serverConfig) {
    new AssetLinkService(FileSystemChecksumServices.service(serverConfig))
  }

  @Provides
  RenderableDecorator<MarkupTemplate> markupTemplateRenderableDecorator(AssetLinkService assetLinkService) {
    RenderableDecorator.of(MarkupTemplate) { c, t ->
      new MarkupTemplate(t.name, t.contentType, [assets: assetLinkService].plus(t.model))
    }
  }

}
