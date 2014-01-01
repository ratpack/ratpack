package ratpack.site

import com.fasterxml.jackson.databind.ObjectReader
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.Scopes
import ratpack.error.ClientErrorHandler
import ratpack.error.ServerErrorHandler
import ratpack.launch.LaunchConfig

@SuppressWarnings(["GrMethodMayBeStatic", "GroovyUnusedDeclaration"])
class SiteModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ClientErrorHandler).toInstance(new SiteErrorHandler())
    bind(ServerErrorHandler).toInstance(new SiteErrorHandler())
    bind(RatpackVersions)
    bind(IssuesService)
  }

  @Provides
  @com.google.inject.Singleton
  GitHubApi gitHubApi(LaunchConfig launchConfig, ObjectReader reader) {
    String authToken = launchConfig.getOther("github.auth", null)
    if (authToken == null) {
      System.err.println("Warning: using anonymous requests to GitHub, may be rate limited (set github.auth other property)")
    }

    String ttlMins = launchConfig.getOther("github.ttl", "5")
    def ttlMinsInt = Integer.parseInt(ttlMins)

    new GitHubApi("https://api.github.com/", authToken, ttlMinsInt, reader)
  }
}
