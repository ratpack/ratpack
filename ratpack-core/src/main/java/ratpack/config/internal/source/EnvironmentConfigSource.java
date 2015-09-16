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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import ratpack.config.ConfigDataBuilder;
import ratpack.config.ConfigSource;
import ratpack.config.EnvironmentParser;
import ratpack.file.FileSystemBinding;
import ratpack.func.Function;
import ratpack.func.Pair;
import ratpack.func.Predicate;
import ratpack.server.internal.ServerEnvironment;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EnvironmentConfigSource implements ConfigSource {
  public static final String DEFAULT_OBJECT_DELIMITER = "__";
  public static final Function<String, String> DEFAULT_MAP_FUNC = camelCase();

  private final ServerEnvironment serverEnvironment;
  private final EnvironmentParser parser;

  public EnvironmentConfigSource(ServerEnvironment serverEnvironment) {
    this(serverEnvironment, ConfigDataBuilder.DEFAULT_ENV_PREFIX);
  }

  public EnvironmentConfigSource(ServerEnvironment serverEnvironment, String prefix) {
    this(serverEnvironment, prefix, DEFAULT_MAP_FUNC);
  }

  public EnvironmentConfigSource(ServerEnvironment serverEnvironment, String prefix, Function<String, String> mapFunc) {
    this(serverEnvironment, new DefaultEnvironmentParser(
      filterAndRemoveKeyPrefix(Preconditions.checkNotNull(prefix)),
      splitObjects(DEFAULT_OBJECT_DELIMITER),
      mapFunc));
  }

  public EnvironmentConfigSource(ServerEnvironment serverEnvironment, EnvironmentParser parser) {
    this.serverEnvironment = serverEnvironment;
    this.parser = parser;
  }

  @Override
  public ObjectNode loadConfigData(ObjectMapper objectMapper, FileSystemBinding fileSystemBinding) throws Exception {
    ObjectNode rootNode = objectMapper.createObjectNode();
    serverEnvironment.getenv().entrySet().stream().map(toPair()).flatMap(getFilterFunc()).map(getPairTokenizerFunc()).forEach(entry -> {
        populate(rootNode, mapPathSegments(entry), 0, entry.getRight());
      }
    );
    return rootNode;
  }

  private List<String> mapPathSegments(Pair<List<String>, String> entry) {
    return entry.getLeft().stream().map(getMapFunc()).collect(Collectors.toList());
  }

  private void populate(ObjectNode node, List<String> path, int pathIndex, String value) {
    String segment = path.get(pathIndex);
    if (pathIndex == path.size() - 1) {
      node.set(segment, TextNode.valueOf(value));
    } else {
      // For environment variables, we don't support array indexing.
      // Thus, if there are remaining segments, it must mean that the parent is an object.
      ObjectNode childNode = (ObjectNode) node.get(segment);
      if (childNode == null) {
        childNode = node.putObject(segment);
      }
      populate(childNode, path, pathIndex + 1, value);
    }
  }

  private java.util.function.Function<Pair<String, String>, Stream<Pair<String, String>>> getFilterFunc() {
    return ((Function<Pair<String, String>, Stream<Pair<String, String>>>) parser::filter).toFunction();
  }

  private java.util.function.Function<Pair<String, String>, Pair<List<String>, String>> getPairTokenizerFunc() {
    return ((Function<Pair<String, String>, Pair<List<String>, String>>) e -> e.mapLeft(parser::tokenize)).toFunction();
  }

  private java.util.function.Function<String, String> getMapFunc() {
    return ((Function<String, String>) parser::map).toFunction();
  }

  public static Function<String, String> camelCase() {
    return Function.fromGuava(CaseFormat.UPPER_UNDERSCORE.converterTo(CaseFormat.LOWER_CAMEL));
  }

  public static Function<Pair<String, String>, Stream<Pair<String, String>>> filterAndRemoveKeyPrefix(String prefix) {
    if (Strings.isNullOrEmpty(prefix)) {
      return Stream::of;
    }
    return entry -> Stream.of(entry).filter(keyStartsWith(prefix).toPredicate()).map(removeKeyPrefix(prefix).toFunction());
  }

  public static Function<String, List<String>> splitObjects(String objectDelimiter) {
    return Splitter.on(objectDelimiter)::splitToList;
  }

  private static Predicate<Pair<String, String>> keyStartsWith(String prefix) {
    return entry -> entry.getLeft().startsWith(prefix);
  }

  private static Function<Pair<String, String>, Pair<String, String>> removeKeyPrefix(String prefix) {
    return entry -> entry.mapLeft(key -> key.substring(prefix.length(), key.length()));
  }

  private static java.util.function.Function<Map.Entry<String, String>, Pair<String, String>> toPair() {
    return e -> Pair.of(e.getKey(), e.getValue());
  }
}
