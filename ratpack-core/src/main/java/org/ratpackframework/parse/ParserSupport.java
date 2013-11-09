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

package org.ratpackframework.parse;

import static org.ratpackframework.util.internal.Types.findImplParameterTypeAtIndex;

abstract public class ParserSupport<T, P extends Parse<T>> implements Parser<T, P> {

  private final Class<P> parseType;
  private final Class<T> parsedType;

  protected ParserSupport() {
    this.parsedType = findImplParameterTypeAtIndex(getClass(), ParserSupport.class, 0);
    this.parseType = findImplParameterTypeAtIndex(getClass(), ParserSupport.class, 1);
  }

  @Override
  public Class<P> getParseType() {
    return parseType;
  }

  @Override
  public Class<T> getParsedType() {
    return parsedType;
  }
}
