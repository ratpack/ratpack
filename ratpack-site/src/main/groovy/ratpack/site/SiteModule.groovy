/*
 * Copyright 2015 the original author or authors.
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

package ratpack.site

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Provides
import groovy.util.logging.Slf4j
import ratpack.file.internal.FileSystemChecksumServices
import ratpack.groovy.template.MarkupTemplate
import ratpack.guice.ConfigurableModule
import ratpack.http.client.HttpClient
import ratpack.render.RenderableDecorator
import ratpack.server.ServerConfig
import ratpack.site.github.*

import javax.inject.Provider
import javax.inject.Singleton
import java.time.Duration

@Slf4j
@SuppressWarnings(["GrMethodMayBeStatic", "GroovyUnusedDeclaration"])
class SiteModule extends ConfigurableModule<GitHubConfig> {

  static class GitHubConfig {
    boolean enabled = true
    String auth
    Duration ttl = Duration.ofMinutes(5)
    Duration errorTimeout = Duration.ofMinutes(1)
    String url = "https://api.github.com/"
  }

  @Override
  protected void configure() {
    bind(RatpackVersions)
    bind(SiteErrorHandler)
  }

  @Provides
  @Singleton
  GitHubApi gitHubApi(GitHubConfig config, ObjectMapper mapper, HttpClient httpClient) {
    String authToken = config.auth
    if (authToken == null) {
      log.warn "Using anonymous requests to GitHub, may be rate limited (set github.auth property)"
    }
    String url = config.url
    new GitHubApi(url, authToken, mapper.reader(), httpClient)
  }

  @Provides
  @Singleton
  GitHubData gitHubData(GitHubConfig config, Provider<GitHubApi> apiProvider) {
    config.enabled ? new GitHubDataCache(config.ttl, config.errorTimeout, new ApiBackedGitHubData(apiProvider.get())) : new NullGitHubData()
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
