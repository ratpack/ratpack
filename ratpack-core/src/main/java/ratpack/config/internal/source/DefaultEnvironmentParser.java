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

package ratpack.config.internal.source;

import ratpack.config.EnvironmentParser;
import ratpack.func.Function;
import ratpack.func.Pair;

import java.util.List;
import java.util.stream.Stream;

public class DefaultEnvironmentParser implements EnvironmentParser {
  private final Function<Pair<String, String>, Stream<Pair<String, String>>> filterFunc;
  private final Function<String, List<String>> tokenizeFunc;
  private final Function<String, String> mapFunc;

  DefaultEnvironmentParser(Function<Pair<String, String>, Stream<Pair<String, String>>> filterFunc,
                              Function<String, List<String>> tokenizeFunc,
                              Function<String, String> mapFunc) {
    this.filterFunc = filterFunc;
    this.tokenizeFunc = tokenizeFunc;
    this.mapFunc = mapFunc;
  }

  @Override
  public Stream<Pair<String, String>> filter(Pair<String, String> entry) throws Exception {
    return filterFunc.apply(entry);
  }

  @Override
  public List<String> tokenize(String name) throws Exception {
    return tokenizeFunc.apply(name);
  }

  @Override
  public String map(String segment) throws Exception {
    return mapFunc.apply(segment);
  }
}
