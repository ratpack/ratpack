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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import ratpack.func.Function;
import ratpack.func.Pair;
import ratpack.util.ExceptionUtils;

import java.util.List;
import java.util.Map;

public class EnvironmentVariablesConfigurationSource extends FlatToNestedConfigurationSource {
  public static final String DEFAULT_PREFIX = "ratpack_";

  private final ImmutableMap<String, String> data;

  public EnvironmentVariablesConfigurationSource() {
    this(DEFAULT_PREFIX);
  }

  public EnvironmentVariablesConfigurationSource(String prefix) {
    this(prefix, System.getenv());
  }

  public EnvironmentVariablesConfigurationSource(String prefix, Map<String, String> data) {
    super(prefix);
    this.data = ImmutableMap.copyOf(data);
  }

  @Override
  Iterable<Pair<String, String>> loadRawData() {
    return Iterables.transform(data.entrySet(), entry -> Pair.of(entry.getKey(), entry.getValue()));
  }

  @Override
  Function<String, Iterable<String>> getKeyTokenizer() {
    return splitByDelimiter("_");
  }

  @Override
  protected Function<Iterable<Pair<String, String>>, Iterable<Pair<String, String>>> transformData() {
    List<Pair<String, String>> globalEntries = Lists.newLinkedList();
    // Special handling for PORT environment variable to support common usage in PAAS systems.
    String globalPort = data.get("PORT");
    if (!Strings.isNullOrEmpty(globalPort)) {
      globalEntries.add(Pair.of("port", globalPort));
    }
    try {
      return super.transformData().andThen(localEntries -> Iterables.concat(globalEntries, localEntries));
    } catch (Exception ex) {
      throw ExceptionUtils.uncheck(ex);
    }
  }
}
