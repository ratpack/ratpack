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

package ratpack.site.github;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import ratpack.exec.Promise;
import ratpack.exec.util.ParallelBatch;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.collect.Iterables.*;

@Singleton
public class RatpackVersions {

  public static final Pattern FINAL_VERSION_PATTERN = Pattern.compile("\\d+\\.\\d+\\.\\d+(-rc-\\d+)?");
  private final GitHubData gitHubData;

  @Inject
  public RatpackVersions(GitHubData gitHubData) {
    this.gitHubData = gitHubData;
  }

  public Promise<All> getAll() {
    return ParallelBatch.of(gitHubData.getReleasedVersions(), gitHubData.getUnreleasedVersions())
      .yield()
      .map(r -> new All(r.get(0), r.get(1)));
  }

  @SuppressWarnings("StaticPseudoFunctionalStyleMethod")
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
      return Iterables.find(released, v -> FINAL_VERSION_PATTERN.matcher(v.getVersion()).matches());
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

    public final List<RatpackVersion> getAll() {
      return ImmutableList.<RatpackVersion>builder().addAll(unreleased).addAll(released).build();
    }
  }
}
