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

package ratpack.path.internal;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ratpack.path.PathBinder;
import ratpack.path.PathBinding;
import ratpack.util.ExceptionUtils;

import java.net.URLDecoder;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TokenPathBinder implements PathBinder {

  private final ImmutableList<String> tokenNames;
  private final Pattern regex;

  private static final LoadingCache<CacheKey, Optional<PathBinding>> CACHE = CacheBuilder.newBuilder().maximumSize(2048).build(new CacheLoader<CacheKey, Optional<PathBinding>>() {
    @Override
    public Optional<PathBinding> load(CacheKey key) throws Exception {
      String path = key.path;
      if (key.parent != null) {
        path = key.parent.getPastBinding();
      }
      Matcher matcher = key.binder.regex.matcher(path);
      if (matcher.matches()) {
        MatchResult matchResult = matcher.toMatchResult();
        String boundPath = matchResult.group(1);
        ImmutableMap.Builder<String, String> paramsBuilder = ImmutableMap.builder();
        int i = 2;
        for (String name : key.binder.tokenNames) {
          String value = matchResult.group(i++);
          if (value != null) {
            paramsBuilder.put(name, URLDecoder.decode(replace(value, "+", "%2B"), "UTF-8"));
          }
        }
        return Optional.of(new DefaultPathBinding(path, boundPath, paramsBuilder.build(), key.parent));
      } else {
        return Optional.empty();
      }
    }
  });

  protected TokenPathBinder(ImmutableList<String> tokenNames, Pattern regex) {
    this.tokenNames = tokenNames;
    this.regex = regex;
  }

  public Optional<PathBinding> bind(String path, PathBinding parentBinding) {
    try {
      return CACHE.get(new CacheKey(this, path, parentBinding));
    } catch (ExecutionException e) {
      throw ExceptionUtils.uncheck(e);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TokenPathBinder that = (TokenPathBinder) o;

    return regex.equals(that.regex) && tokenNames.equals(that.tokenNames);
  }

  @Override
  public int hashCode() {
    int result = tokenNames.hashCode();
    result = 31 * result + regex.hashCode();
    return result;
  }

  private static class CacheKey {
    private final TokenPathBinder binder;
    private final String path;
    private final PathBinding parent;

    public CacheKey(TokenPathBinder binder, String path, PathBinding parent) {
      this.binder = binder;
      this.path = path;
      this.parent = parent;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      CacheKey cacheKey = (CacheKey) o;

      return binder.equals(cacheKey.binder) && !(parent != null ? !parent.equals(cacheKey.parent) : cacheKey.parent != null) && path.equals(cacheKey.path);
    }

    @Override
    public int hashCode() {
      int result = binder.hashCode();
      result = 31 * result + path.hashCode();
      result = 31 * result + (parent != null ? parent.hashCode() : 0);
      return result;
    }
  }

  private static String replace(String source, String find, String replace) {
    if (source == null) {
      return null;
    }
    int i = 0;
    if ((i = source.indexOf(find, i)) >= 0) {
      char[] sourceArray = source.toCharArray();
      char[] nsArray = replace.toCharArray();
      int oLength = find.length();
      StringBuilder buf = new StringBuilder(sourceArray.length);
      buf.append(sourceArray, 0, i).append(nsArray);
      i += oLength;
      int j = i;
      // Replace all remaining instances of oldString with newString.
      while ((i = source.indexOf(find, i)) > 0) {
        buf.append(sourceArray, j, i - j).append(nsArray);
        i += oLength;
        j = i;
      }
      buf.append(sourceArray, j, sourceArray.length - j);
      source = buf.toString();
      buf.setLength(0);
    }
    return source;
  }
}
