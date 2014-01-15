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

  rx.Observable<All> getAll() {
    rx.Observable.zip(gitHubData.releasedVersions, gitHubData.unreleasedVersions) { released, unreleased ->
      new All(released as List<RatpackVersion>, unreleased as List<RatpackVersion>)
    }
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

  rx.Observable<RatpackVersion> getSnapshot() {
    firstOr(gitHubData.unreleasedVersions, null)
  }

  rx.Observable<RatpackVersion> getCurrent() {
    firstOr(gitHubData.releasedVersions, null)
  }

  private static <T> rx.Observable<T> firstOr(rx.Observable<? extends Collection<T>> observable, T defaultValue) {
    observable.map {
      Collection<T> collection = it as Collection<T>
      collection.empty ? defaultValue : collection.first()
    }
  }

}
