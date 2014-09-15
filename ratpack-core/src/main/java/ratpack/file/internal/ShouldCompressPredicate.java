/*
 * Copyright 2014 the original author or authors.
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

package ratpack.file.internal;

import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.UncheckedExecutionException;
import ratpack.func.Pair;

import java.util.concurrent.ExecutionException;

import static ratpack.util.ExceptionUtils.toException;
import static ratpack.util.ExceptionUtils.uncheck;

public class ShouldCompressPredicate implements Predicate<Pair<Long, String>> {

  private final long compressionMinSize;
  private final ImmutableSet<String> compressionMimeTypeWhiteList;
  private final ImmutableSet<String> compressionMimeTypeBlackList;

  private final LoadingCache<String, Boolean> cache = CacheBuilder.newBuilder().build(new CacheLoader<String, Boolean>() {
    @Override
    public Boolean load(@SuppressWarnings("NullableProblems") String key) throws Exception {
      for (String s : compressionMimeTypeWhiteList) {
        if (key.startsWith(s)) {
          return true;
        }
      }
      for (String s : compressionMimeTypeBlackList) {
        if (key.startsWith(s)) {
          return false;
        }
      }
      return true;
    }
  });

  public ShouldCompressPredicate(long compressionMinSize, ImmutableSet<String> compressionMimeTypeWhiteList, ImmutableSet<String> compressionMimeTypeBlackList) {
    this.compressionMinSize = compressionMinSize;
    this.compressionMimeTypeWhiteList = compressionMimeTypeWhiteList;
    this.compressionMimeTypeBlackList = compressionMimeTypeBlackList;
  }

  @Override
  public boolean apply(Pair<Long, String> fileDetails) {
    if (fileDetails.left < compressionMinSize) {
      return false;
    }

    try {
      return cache.get(fileDetails.right);
    } catch (ExecutionException | UncheckedExecutionException e) {
      throw uncheck(toException(e.getCause()));
    }
  }

}
