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

package ratpack.config;

import ratpack.func.Pair;

import java.util.List;
import java.util.stream.Stream;

/**
 * Strategy for parsing a set of environment variables into a form appropriate for use in a {@link ConfigSource}.
 * The methods are called in order:
 *
 * <ol>
 *   <li>{@link #filter(ratpack.func.Pair)}</li>
 *   <li>{@link #tokenize(String)}</li>
 *   <li>{@link #map(String)}</li>
 * </ol>
 */
public interface EnvironmentParser {
  /**
   * Provides an opportunity to remove environment variables from parsing by the remainder of the pipeline.
   * The pair can be modified if desired as well, such as to remove prefixes.
   *
   * @param entry an environment variable, encoded with the name on the left and the value on the right
   * @return a stream containing the desired entries (if any) for this entry
   * @throws Exception any
   *
   * @see Pair#mapLeft(ratpack.func.Function)
   */
  Stream<Pair<String, String>> filter(Pair<String, String> entry) throws Exception;

  /**
   * Splits the name of an environment variable into per-object segments.
   * For example, if you want name {@code SERVER__URL} to mean an object "SERVER" and a field "URL", you would return a two element list {@code ["SERVER", "URL"]}.
   *
   * @param name the name of the environment variable
   * @return the per-object segments for the environment variable, starting with the segments closest to the root
   * @throws Exception any
   */
  List<String> tokenize(String name) throws Exception;

  /**
   * Transforms a segment.  Most commonly, this is used to convert between case formats.
   *
   * @param segment a segment of the object path
   * @return the transformed segment
   * @throws Exception any
   *
   * @see com.google.common.base.CaseFormat
   */
  String map(String segment) throws Exception;
}
