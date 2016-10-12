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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http.QueryStringDecoder;
import ratpack.path.InvalidPathEncodingException;
import ratpack.path.PathBinder;
import ratpack.path.PathBinding;

import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TokenPathBinder implements PathBinder {

  private final ImmutableList<String> tokenNames;
  private final String target;
  private final Pattern regex;

  protected TokenPathBinder(ImmutableList<String> tokenNames, String target, Pattern regex) {
    this.tokenNames = tokenNames;
    this.target = target;
    this.regex = regex;
  }

  public Optional<PathBinding> bind(PathBinding parentBinding) {
    Matcher matcher = regex.matcher(parentBinding.getPastBinding());
    if (matcher.matches()) {
      MatchResult matchResult = matcher.toMatchResult();
      String boundPath = matchResult.group(1);
      ImmutableMap.Builder<String, String> paramsBuilder = ImmutableMap.builder();
      int i = 2;
      for (String name : tokenNames) {
        String value = matchResult.group(i++);
        if (value != null) {
          paramsBuilder.put(name, decodeURIComponent(value));
        }
      }

      return Optional.of(new DefaultPathBinding(boundPath, paramsBuilder.build(), parentBinding, target));
    } else {
      return Optional.empty();
    }
  }

  private String decodeURIComponent(String s) {
    try {
      return QueryStringDecoder.decodeComponent(s.replace("+", "%2B"));
    } catch (IllegalArgumentException cause) {
      throw new InvalidPathEncodingException(cause);
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
}
