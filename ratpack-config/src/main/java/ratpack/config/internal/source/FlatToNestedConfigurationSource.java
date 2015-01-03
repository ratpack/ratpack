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
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteSource;
import ratpack.api.Nullable;
import ratpack.config.ConfigurationSource;
import ratpack.func.Function;
import ratpack.func.Pair;
import ratpack.util.ExceptionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public abstract class FlatToNestedConfigurationSource implements ConfigurationSource {
  private final String prefix;

  public FlatToNestedConfigurationSource(@Nullable String prefix) {
    this.prefix = prefix;
    // TODO: pull in richer impl from https://github.com/danveloper/config-binding/blob/master/src/main/java/config/PropertiesConfigurationSource.java
  }

  private void putValue(ObjectMapper objectMapper, Iterable<String> keyParts, String value, ObjectNode node) {
    String curPart = Iterables.getFirst(keyParts, null);
    if (Iterables.size(keyParts) == 1) {
      node.set(curPart, TextNode.valueOf(value));
    } else {
      ObjectNode childNode = (ObjectNode) node.get(curPart);
      if (childNode == null) {
        childNode = objectMapper.createObjectNode();
        node.set(curPart, childNode);
      }
      putValue(objectMapper, Iterables.skip(keyParts, 1), value, childNode);
    }
  }

  @Override
  public ObjectNode loadConfigurationData(ObjectMapper objectMapper) {
    try {
      Function<String, Iterable<String>> keyTokenizer = getKeyTokenizer();
      ObjectNode rootNode = objectMapper.createObjectNode();
      for (Pair<String, String> entry : transformData().apply(loadRawData())) {
        Iterable<String> keyParts = keyTokenizer.apply(entry.left);
        putValue(objectMapper, keyParts, entry.right, rootNode);
      }
      return rootNode;
    } catch (Exception ex) {
      throw ExceptionUtils.uncheck(ex);
    }
  }

  private static Properties load(ByteSource byteSource) {
    Properties properties = new Properties();
    try (InputStream inputStream = byteSource.openStream()) {
      properties.load(inputStream);
    } catch (IOException ex) {
      throw ExceptionUtils.uncheck(ex);
    }
    return properties;
  }

  abstract Iterable<Pair<String, String>> loadRawData();

  protected Function<Iterable<Pair<String, String>>, Iterable<Pair<String, String>>> transformData() {
    if (!Strings.isNullOrEmpty(prefix)) {
      return entries -> FluentIterable.from(entries)
        .filter(entry -> entry.getLeft().startsWith(prefix))
        .transform(
          entry -> {
            try {
              return entry.mapLeft(key -> key.substring(prefix.length(), key.length()));
            } catch (Exception ex) {
              throw ExceptionUtils.uncheck(ex);
            }
          }
        );
    }
    return entries -> entries;
  }

  abstract Function<String, Iterable<String>> getKeyTokenizer();

  protected Function<String, Iterable<String>> splitByDelimiter(String delimiter) {
    return Splitter.on(delimiter)::splitToList;
  }
}
