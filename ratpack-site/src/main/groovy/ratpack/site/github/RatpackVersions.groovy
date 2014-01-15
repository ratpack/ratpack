package ratpack.site.github

import com.google.inject.Provider
import groovy.transform.CompileStatic
import ratpack.background.Background

import javax.inject.Inject
import java.util.concurrent.Callable

@CompileStatic
@javax.inject.Singleton
class RatpackVersions {

  private final GitHubData gitHubData
  private final Provider<Background> backgroundProvider

  @Inject
  RatpackVersions(GitHubData gitHubData, Provider<Background> backgroundProvider) {
    this.backgroundProvider = backgroundProvider
    this.gitHubData = gitHubData
  }

  private <T> Background.SuccessOrError<T> background(Callable<T> callable) {
    backgroundProvider.get().exec(callable)
  }

  Background.SuccessOrError<All> getAll() {
    background { new All(gitHubData.releasedVersions, gitHubData.unreleasedVersions) }
  }

  static class All {
    final List<RatpackVersion> released
    final List<RatpackVersion> unreleased

    All(List<RatpackVersion> released, List<RatpackVersion> unreleased) {
      this.released = released
      this.unreleased = unreleased
    }

    RatpackVersion find(String version) {
      def cl = { RatpackVersion it -> it.version == version }
      released.find(cl) ?: unreleased.find(cl)
    }
  }

  Background.SuccessOrError<RatpackVersion> getSnapshot() {
    background { gitHubData.unreleasedVersions.empty ? null : gitHubData.unreleasedVersions.first() }
  }

  Background.SuccessOrError<RatpackVersion> getCurrent() {
    background { gitHubData.releasedVersions.empty ? null : gitHubData.releasedVersions.first() }
  }

}
