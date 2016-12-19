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

package ratpack.site.github

import com.google.common.collect.Maps
import ratpack.exec.Promise
import ratpack.util.Types

import java.time.Duration
import java.util.concurrent.ConcurrentMap

class GitHubDataCache implements GitHubData {

  private final ConcurrentMap<Key<?>, Promise<?>> cache = Maps.newConcurrentMap()

  private final Duration cacheFor
  private final Duration errorTimeout
  private final GitHubData data

  GitHubDataCache(Duration cacheFor, Duration errorTimeout, GitHubData data) {
    this.cacheFor = cacheFor
    this.errorTimeout = errorTimeout
    this.data = data
  }

  private <T> Promise<T> get(Key<T> key) {
    def promise = key.factory().cacheResultFor { it.error ? errorTimeout : cacheFor }
    (cache.putIfAbsent(key, promise) ?: promise).map { Types.<T> cast(it) }
  }

  @Override
  Promise<List<RatpackVersion>> getReleasedVersions() {
    get(new Key<List<RatpackVersion>>("released-versions", { data.releasedVersions }))
  }

  @Override
  Promise<List<RatpackVersion>> getUnreleasedVersions() {
    get(new Key<List<RatpackVersion>>("unreleased-versions", { data.unreleasedVersions }))
  }

  @Override
  Promise<IssueSet> closed(RatpackVersion version) {
    get(new Key<IssueSet>("closed-$version.version", { data.closed(version) }))
  }

  @Override
  void forceRefresh() {
    cache.clear()
  }

  private class Key<T> {
    final String id
    final Closure<Promise<T>> factory

    Key(String id, Closure<Promise<T>> factory) {
      this.id = id
      this.factory = factory
    }

    boolean equals(o) {
      if (this.is(o)) {
        return true
      }
      if (getClass() != o.class) {
        return false
      }

      Key key = (Key) o

      id == key.id
    }

    int hashCode() {
      id.hashCode()
    }
  }

}
