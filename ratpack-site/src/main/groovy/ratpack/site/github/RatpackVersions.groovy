package ratpack.site.github

import groovy.transform.CompileStatic

import javax.inject.Inject

@CompileStatic
@javax.inject.Singleton
class RatpackVersions {

  private final GitHubData gitHubData

  @Inject
  RatpackVersions(GitHubData gitHubData) {
    this.gitHubData = gitHubData
  }

  All getAll() {
    new All(gitHubData.releasedVersions, gitHubData.unreleasedVersions)
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

  RatpackVersion getSnapshot() {
    gitHubData.unreleasedVersions.empty ? null : gitHubData.unreleasedVersions.first()
  }

  RatpackVersion getCurrent() {
    gitHubData.releasedVersions.empty ? null : gitHubData.releasedVersions.first()
  }

}
