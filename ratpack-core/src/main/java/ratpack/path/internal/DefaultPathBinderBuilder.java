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
import ratpack.path.PathBinder;
import ratpack.path.PathBinderBuilder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultPathBinderBuilder implements PathBinderBuilder {

  private static final Pattern PLACEHOLDER = Pattern.compile("((?:^|/):(\\w+)\\??:([^/])+)|((?:^|/)::([^/])+)|((?:^|/):(\\w+)\\??)");

  private final ImmutableList.Builder<String> tokensBuilder = ImmutableList.builder();
  private final StringBuilder pattern = new StringBuilder();
  private final StringBuilder description = new StringBuilder();
  private boolean addedOptional;
  private boolean addedToken;

  public PathBinderBuilder tokenWithPattern(String token, String pattern) {
    if (addedOptional) {
      throw new IllegalArgumentException(String.format("Cannot add mandatory parameter %s after optional parameters", token));
    }
    addedToken = true;
    tokensBuilder.add(token);
    this.pattern.append(String.format("(?:(?:^|/)(%s))", pattern));
    appendDescriptionSegment(":").append(token).append(":").append(pattern);
    return this;
  }

  public PathBinderBuilder optionalTokenWithPattern(String token, String pattern) {
    addedOptional = true;
    addedToken = true;
    tokensBuilder.add(token);
    this.pattern.append(String.format("(?:(?:^|/)(%s))?", pattern));
    appendDescriptionSegment(":").append(token).append("?:").append(pattern);
    return this;
  }

  public PathBinderBuilder token(String token) {
    if (addedOptional) {
      throw new IllegalArgumentException(String.format("Cannot add mandatory parameter %s after optional parameters", token));
    }
    addedToken = true;
    tokensBuilder.add(token);
    pattern.append("(?:(?:^|/)([^/?&#]+))");
    appendDescriptionSegment(":").append(token);
    return this;
  }

  public PathBinderBuilder optionalToken(String token) {
    addedOptional = true;
    addedToken = true;
    tokensBuilder.add(token);
    pattern.append("(?:(?:^|/)([^/?&#]*))?");
    appendDescriptionSegment(":").append(token).append("?");
    return this;
  }

  public PathBinderBuilder literalPattern(String pattern) {
    this.pattern.append("(?:(?:^|/)").append("(?:").append(pattern).append("))");
    appendDescriptionSegment("::").append(pattern);
    return this;
  }

  public PathBinderBuilder literal(String literal) {
    this.pattern.append("\\Q").append(literal).append("\\E");
    this.description.append(literal);
    return this;
  }

  private StringBuilder appendDescriptionSegment(String segment) {
    if (description.length() > 0) {
      description.append("/");
    }
    return description.append(segment);
  }

  public PathBinder build(boolean exhaustive) {
    String regex = (addedToken ? "(\\Q\\E" : "(") + pattern + (addedToken ? "\\Q\\E)" : ")") + (exhaustive ? "(?:/|$)" : "(?:/.*)?");
    Pattern compiled = Pattern.compile(regex);
    return new TokenPathBinder(tokensBuilder.build(), description.toString(), compiled);
  }

  public static PathBinder parse(String path, boolean exact) {
    PathBinderBuilder pathBinderBuilder = new DefaultPathBinderBuilder();

    Matcher matchResult = PLACEHOLDER.matcher(path);

    int lastIndex = 0;

    if (matchResult.find()) {
      do {
        int thisIndex = matchResult.start();
        if (thisIndex != lastIndex) {
          pathBinderBuilder.literal(path.substring(lastIndex, thisIndex));
        }
        lastIndex = matchResult.end();
        String component = matchResult.group(0);
        boolean found = false;
        for (PathTokenType type : PathTokenType.values()) {
          if (type.match(component, pathBinderBuilder)) {
            found = true;
            break;
          }
        }
        if (!found) {
          throw new IllegalArgumentException(String.format("Cannot match path %s (%s)", path, component));
        }
      } while (matchResult.find());
      if (lastIndex < path.length()) {
        pathBinderBuilder.literal(path.substring(lastIndex));
      }
    } else {
      pathBinderBuilder.literal(path);
    }
    return pathBinderBuilder.build(exact);
  }

}
