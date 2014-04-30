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
import ratpack.path.PathBuilder;

import java.util.regex.Pattern;

public class DefaultPathBuilder implements PathBuilder {
  private ImmutableList.Builder<String> tokensBuilder = ImmutableList.builder();
  private StringBuilder pattern = new StringBuilder();
  private boolean addedOptional;
  private boolean addedToken;

  public PathBuilder tokenWithPattern(String token, String pattern) {
    if (addedOptional) {
      throw new IllegalArgumentException(String.format("Cannot add mandatory parameter %s after optional parameters", token));
    }
    addedToken = true;
    tokensBuilder.add(token);
    this.pattern.append(String.format("(?:(?:^|/)(%s))", pattern));
    return this;
  }

  public PathBuilder optionalTokenWithPattern(String token, String pattern) {
    addedOptional = true;
    addedToken = true;
    tokensBuilder.add(token);
    this.pattern.append(String.format("(?:(?:^|/)(%s))?", pattern));
    return this;
  }

  public PathBuilder token(String token) {
    if (addedOptional) {
      throw new IllegalArgumentException(String.format("Cannot add mandatory parameter %s after optional parameters", token));
    }
    addedToken = true;
    tokensBuilder.add(token);
    pattern.append("(?:(?:^|/)([^/?&#]+))");
    return this;
  }

  public PathBuilder optionalToken(String token) {
    addedOptional = true;
    addedToken = true;
    tokensBuilder.add(token);
    pattern.append("(?:(?:^|/)([^/?&#]*))?");
    return this;
  }

  public PathBuilder literalPattern(String pattern) {
    this.pattern.append(String.format("(?:%s)", pattern));
    return this;
  }

  public PathBuilder literal(String literal) {
    this.pattern.append(String.format("\\Q%s\\E", literal));
    return this;
  }

  public PathBinder build(boolean exact) {
    StringBuilder output = new StringBuilder().append(addedToken ? "(\\Q\\E" : "(")
                          .append(pattern)
                          .append(addedToken ? "\\Q\\E)" : ")")
                          .append(exact ? "(?:/|$)" : "(?:/.*)?");
    return new TokenPathBinder(tokensBuilder.build(), Pattern.compile(output.toString()));
  }
}