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

package ratpack.path.internal;

import ratpack.path.PathBinderBuilder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum PathTokenType implements PathTokenProcessor {
  OPTIONAL_TOKEN_WITH_PATTERN("/?:(\\w*)\\?:(.+)") {
    public void process(Matcher matcher, PathBinderBuilder builder) {
      builder.optionalTokenWithPattern(matcher.group(1), matcher.group(2));
    }
  },
  LITERAL_WITH_PATTERN("/?::(.+)") {
    public void process(Matcher matcher, PathBinderBuilder builder) {
      builder.literalPattern(matcher.group(1));
    }
  },
  TOKEN_WITH_PATTERN("/?:(\\w*):(.+)") {
    public void process(Matcher matcher, PathBinderBuilder builder) {
      builder.tokenWithPattern(matcher.group(1), matcher.group(2));
    }
  },
  TOKEN("/?:(\\w*)") {
    public void process(Matcher matcher, PathBinderBuilder builder) {
      builder.token(matcher.group(1));
    }
  },
  OPTIONAL_TOKEN("/?:(\\w*)\\?") {
    public void process(Matcher matcher, PathBinderBuilder builder) {
      builder.optionalToken(matcher.group(1));
    }
  };

  public boolean match(String component, PathBinderBuilder builder) {
    Matcher matcher = pattern.matcher(component);
    if (matcher.matches()) {
      process(matcher, builder);
      return true;
    }
    return false;
  }

  private Pattern pattern;

  PathTokenType(String pattern) {
    this.pattern = Pattern.compile(pattern);
  }
}
