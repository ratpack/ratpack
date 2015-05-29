package ratpack.site.github;

import ratpack.exec.ExecControl;
import ratpack.exec.Execution;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.util.Types;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.collect.Iterables.*;

@Singleton
public class RatpackVersions {

  private final GitHubData gitHubData;
  private final ExecControl execControl;

  @Inject
  public RatpackVersions(GitHubData gitHubData, ExecControl execControl) {
    this.execControl = execControl;
    this.gitHubData = gitHubData;
  }

  public Promise<All> getAll() {
    // TODO need better support in Ratpack for this kind of thing
    return execControl.promise(f -> {
      AtomicInteger counter = new AtomicInteger(2);
      Object[] versions = new Object[2];


      Action<Execution> onComplete = e -> {
        if (counter.decrementAndGet() == 0) {
          f.success(new All(Types.cast(versions[0]), Types.cast(versions[1])));
        }
      };

      AtomicBoolean error = new AtomicBoolean();

      Action<Throwable> onError = t -> {
        if (error.compareAndSet(false, true)) {
          f.error(t);
        }
      };

      execControl.fork()
        .onError(onError)
        .onComplete(onComplete)
        .start(e -> gitHubData.getReleasedVersions().then(l -> versions[0] = l));

      execControl.fork()
        .onError(onError)
        .onComplete(onComplete)
        .start(e -> gitHubData.getUnreleasedVersions().then(l -> versions[1] = l));
    });
  }

  public static class All {

    private final List<RatpackVersion> released;
    private final List<RatpackVersion> unreleased;

    public All(List<RatpackVersion> released, List<RatpackVersion> unreleased) {
      this.released = released;
      this.unreleased = unreleased;
    }

    public RatpackVersion version(final String version) {
      return find(concat(released, unreleased), v -> v.getVersion().equals(version), null);
    }

    public RatpackVersion getSnapshot() {
      return getFirst(unreleased, null);
    }

    public RatpackVersion getCurrent() {
      return getFirst(released, null);
    }

    public boolean isReleased(final String version) {
      return any(released, v -> v.getVersion().equals(version));
    }

    public boolean isUnreleased(final String version) {
      return any(unreleased, v -> v.getVersion().equals(version));
    }

    public final List<RatpackVersion> getReleased() {
      return released;
    }

    public final List<RatpackVersion> getUnreleased() {
      return unreleased;
    }
  }
}
