/*
 * Copyright 2013 the original author or authors.
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

package org.ratpackframework.path.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.ratpackframework.path.PathBinder;
import org.ratpackframework.path.PathBinding;
import org.ratpackframework.util.internal.Validations;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TokenPathBinder implements PathBinder {

  private final ImmutableList<String> tokenNames;
  private final Pattern regex;

  public TokenPathBinder(String path, boolean exact) {
    Validations.noLeadingForwardSlash(path, "token path");

    ImmutableList.Builder<String> namesBuilder = ImmutableList.builder();
    String pattern = Pattern.quote(path);

    Pattern placeholderPattern = Pattern.compile("(/?:(\\w+)\\?*)");
    Matcher matchResult = placeholderPattern.matcher(path);
    boolean hasOptional = false;
    while (matchResult.find()) {
      String name = matchResult.group(1);
      boolean optional = name.contains("?");

      hasOptional = hasOptional || optional;
      if (hasOptional && !optional) {
        throw new IllegalArgumentException(String.format("path %s should not define mandatory parameters after an optional parameter", path));
      }

      pattern = pattern.replaceFirst(Pattern.quote(name), "\\\\E/?([^/?&#]+)" + ((optional) ? "?" : "") + "\\\\Q");
      namesBuilder.add(matchResult.group(2));
    }

    StringBuilder patternBuilder = new StringBuilder("(").append(pattern).append(")");
    if (!exact) {
      patternBuilder.append("(?:/.*)?");
    }

    this.regex = Pattern.compile(patternBuilder.toString());
    this.tokenNames = namesBuilder.build();
  }

  public PathBinding bind(String path, PathBinding parentBinding) {
    if (parentBinding != null) {
      path = parentBinding.getPastBinding();
    }
    Matcher matcher = regex.matcher(path);
    if (matcher.matches()) {
      MatchResult matchResult = matcher.toMatchResult();
      String boundPath = matchResult.group(1);
      ImmutableMap.Builder<String, String> paramsBuilder = ImmutableMap.builder();
      int i = 2;
      for (String name : tokenNames) {
        String value = matchResult.group(i++);
        if (value != null) {
          paramsBuilder.put(name, value);
        }
      }

      return new DefaultPathBinding(path, boundPath, paramsBuilder.build(), parentBinding);
    } else {
      return null;
    }
  }
}
