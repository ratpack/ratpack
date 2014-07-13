package ratpack.site.github

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import ratpack.exec.ExecControl
import ratpack.rx.RxRatpack

import javax.inject.Inject

@CompileStatic
@javax.inject.Singleton
class RatpackVersions {

  private final GitHubData gitHubData
  private final ExecControl execControl

  @Inject
  RatpackVersions(GitHubData gitHubData, ExecControl execControl) {
    this.execControl = execControl
    this.gitHubData = gitHubData
  }

  static class VersionSet<T> {
    boolean released
    T versions

    VersionSet(boolean released, T versions) {
      this.released = released
      this.versions = versions
    }
  }

  @CompileDynamic
  rx.Observable<All> getAll() {
    rx.Observable<VersionSet<rx.Observable<List<RatpackVersion>>>> released = rx.Observable.from(new VersionSet(true, gitHubData.releasedVersions))
    rx.Observable<VersionSet<rx.Observable<List<RatpackVersion>>>> unreleased = rx.Observable.from(new VersionSet(false, gitHubData.unreleasedVersions))
    rx.Observable<VersionSet<rx.Observable<List<RatpackVersion>>>> source = rx.Observable.concat(released, unreleased)

    rx.Observable.Operator<VersionSet<rx.Observable<List<RatpackVersion>>>, VersionSet<rx.Observable<List<RatpackVersion>>>> forkOperation = RxRatpack.forkOnNext(execControl)
    def processing = source.
      lift(forkOperation).
      flatMap { set ->
        set.versions.map {
          new VersionSet<List<RatpackVersion>>(set.released, it)
        }
      }.
      serialize().
      toList().
      map { List<VersionSet<List<RatpackVersion>>> it ->
        new All(it.find { it.released }.versions, it.find { !it.released }.versions)
      }

    RxRatpack.forkAndJoin(execControl, processing)
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

    RatpackVersion getSnapshot() {
      unreleased ? unreleased.first() : null
    }

    RatpackVersion getCurrent() {
      released ? released.first() : null
    }
  }

}
