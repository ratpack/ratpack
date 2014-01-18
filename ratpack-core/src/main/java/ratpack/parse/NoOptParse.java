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

package ratpack.parse;

/**
 * A generic parse type that can be used when parsers do not need any extra information from parse objects other than type.
 * <p>
 * Use {@link #to(Class)} to create instances.
 *
 * @param <T> The type of object to parse to
 */
public final class NoOptParse<T> implements Parse<T> {

  private final Class<T> type;

  private NoOptParse(Class<T> type) {
    this.type = type;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<T> getType() {
    return type;
  }

  /**
   * Creates a no option parse for the given type.
   *
   * @param type The type to parse to
   * @param <T> The type to parse to
   * @return An no option parser for the given type
   */
  public static <T> NoOptParse<T> to(Class<T> type) {
    return new NoOptParse<>(type);
  }

}
