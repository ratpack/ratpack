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

package ratpack.path;

/**
 * A builder to generate a PathBinder from a list of tokens and literals (with and
 * without regular expression patterns)
 *
 * @see PathBinder
 */
public interface PathBuilder {
  /**
   * Add a regular expression parameterized named token to the path.
   *
   * @param token The name of the token
   * @param pattern The valid regex pattern
   * @return The builder
   */
  PathBuilder tokenWithPattern(String token, String pattern);

  /**
   * Add a regular expression parameterized named optional token to the path.
   *
   * @param token The name of the optional token
   * @param pattern The valid regex pattern
   * @return The builder
   */
  PathBuilder optionalTokenWithPattern(String token, String pattern);

  /**
   * Add a token to the path.
   *
   * @param token The name of the token
   * @return The builder
   */
  PathBuilder token(String token);

  /**
   * Add an optional token to the path.
   *
   * @param token The name of the optional token
   * @return The builder
   */
  PathBuilder optionalToken(String token);

  /**
   * Add a regular expression parameterized literal element to the path.
   *
   * @param pattern The valid regex pattern
   * @return The builder
   */
  PathBuilder literalPattern(String pattern);

  /**
   * Add a literal to the path.
   *
   * @param literal The literal path component
   * @return The builder
   */
  PathBuilder literal(String literal);

  /**
   * Generate a {@link PathBinder} from the contents of the builder.
   *
   * @param exact Whether this path should be an exact match
   * @return A PathBinder instance
   */
  PathBinder  build(boolean exact);
}