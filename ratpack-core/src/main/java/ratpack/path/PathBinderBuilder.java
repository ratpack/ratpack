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
 * A builder to generate a {@link PathBinder} from a list of tokens and literals.
 *
 * @see PathBinder#of(boolean, ratpack.func.Action)
 * @see PathBinder#builder()
 */
public interface PathBinderBuilder {
  /**
   * Add a regular expression parameterized named token to the path.
   *
   * @param token the name of the token
   * @param pattern the valid regex pattern
   * @return this
   */
  PathBinderBuilder tokenWithPattern(String token, String pattern);

  /**
   * Add a regular expression parameterized named optional token to the path.
   *
   * @param token the name of the optional token
   * @param pattern the valid regex pattern
   * @return this
   */
  PathBinderBuilder optionalTokenWithPattern(String token, String pattern);

  /**
   * Add a token to the path.
   *
   * @param token the name of the token
   * @return this
   */
  PathBinderBuilder token(String token);

  /**
   * Add an optional token to the path.
   *
   * @param token the name of the optional token
   * @return this
   */
  PathBinderBuilder optionalToken(String token);

  /**
   * Add a regular expression parameterized literal element to the path.
   *
   * @param pattern the valid regex pattern
   * @return this
   */
  PathBinderBuilder literalPattern(String pattern);

  /**
   * Add a literal to the path.
   *
   * @param literal the literal path component
   * @return this
   */
  PathBinderBuilder literal(String literal);

  /**
   * Generate a {@link PathBinder} from the contents of the builder.
   *
   * @param exhaustive whether this path should be an exact match
   * @return a new {@link PathBinder} based on the state of this builder
   */
  PathBinder build(boolean exhaustive);
}
