package ratpack.site

import groovy.transform.CompileStatic

import javax.inject.Inject

@CompileStatic
@javax.inject.Singleton
class RatpackVersions {

  private final GitHubApi githubApi

  @Inject
  RatpackVersions(GitHubApi githubApi) {
    this.githubApi = githubApi
  }

  List<RatpackVersion> getReleasedVersions() {
    RatpackVersion.fromJson(githubApi.milestones(state: "closed", sort: "due_date"))
  }

  List<RatpackVersion> getUnreleasedVersions() {
    RatpackVersion.fromJson(githubApi.milestones(state: "open", sort: "due_date", direction: "asc"))
  }

  All getAll() {
    new All(releasedVersions, unreleasedVersions)
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
    unreleasedVersions.empty ? null : unreleasedVersions.first()
  }

  RatpackVersion getCurrent() {
    releasedVersions.empty ? null : releasedVersions.first()
  }
}
