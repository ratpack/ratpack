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

package ratpack.parse.internal;

import com.google.common.base.Predicate;
import ratpack.parse.Parse;
import ratpack.parse.Parser;

public class ParserForParsePredicate implements Predicate<Parser<?>> {
  private final Parse<?, ?> parse;
  private final String contentType;

  public ParserForParsePredicate(Parse<?, ?> parse, String contentType) {
    this.parse = parse;
    this.contentType = contentType;
  }

  @Override
  public boolean apply(Parser<?> parser) {
    return contentType.equalsIgnoreCase(parser.getContentType()) && parser.getOptsType().isInstance(parse.getOpts());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ParserForParsePredicate that = (ParserForParsePredicate) o;
    return contentType.equalsIgnoreCase(that.contentType) && parse.equals(that.parse);
  }

  @Override
  public int hashCode() {
    int result = contentType.hashCode();
    result = 31 * result + parse.hashCode();
    return result;
  }
}
