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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import ratpack.config.ConfigurationSource;
import ratpack.config.internal.util.PathUtil;
import ratpack.func.Function;
import ratpack.func.Pair;
import ratpack.util.ExceptionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

public class PropertiesConfigurationSource implements ConfigurationSource {
  private final Optional<String> prefix;
  private final Properties properties;

  public PropertiesConfigurationSource(Optional<String> prefix, Properties properties) {
    this.prefix = prefix;
    this.properties = properties;
  }

  public PropertiesConfigurationSource(Optional<String> prefix, ByteSource byteSource) {
    this(prefix, load(byteSource));
  }

  public PropertiesConfigurationSource(ByteSource byteSource) {
    this(Optional.empty(), byteSource);
  }

  public PropertiesConfigurationSource(URL url) {
    this(Optional.empty(), Resources.asByteSource(url));
  }

  public PropertiesConfigurationSource(Path path) {
    this(Optional.empty(), Files.asByteSource(path.toFile()));
  }

  public PropertiesConfigurationSource(String pathOrUrl) {
    this(Optional.empty(), PathUtil.asByteSource(pathOrUrl));
  }

  public PropertiesConfigurationSource(Properties properties) {
    this(Optional.empty(), properties);
  }

  @Override
  public ObjectNode loadConfigurationData(ObjectMapper objectMapper) throws Exception {
    ObjectNode rootNode = objectMapper.createObjectNode();
    Stream<Pair<String, String>> pairs = properties.stringPropertyNames().stream().map(key -> Pair.of(key, properties.getProperty(key)));
    if (prefix.isPresent()) {
      pairs = pairs
        .filter(p -> p.left.startsWith(prefix.get()))
        .map(((Function<Pair<String, String>, Pair<String, String>>) p -> p.mapLeft(s -> s.substring(prefix.get().length()))).toFunction());
    }
    pairs.forEach(p -> populate(rootNode, p.left, p.right));
    return rootNode;
  }

  private void populate(ObjectNode node, String key, String value) {
    int nextDot = key.indexOf('.');
    int nextOpenBracket = key.indexOf('[');
    boolean hasDelimiter = nextDot != -1;
    boolean hasIndexing = nextOpenBracket != -1;
    if (hasDelimiter && (!hasIndexing || (nextDot < nextOpenBracket))) {
      String fieldName = key.substring(0, nextDot);
      String remainingKey = key.substring(nextDot + 1);
      ObjectNode childNode = (ObjectNode) node.get(fieldName);
      if (childNode == null) {
        childNode = node.putObject(fieldName);
      }
      populate(childNode, remainingKey, value);
    } else if (hasIndexing) {
      int nextCloseBracket = key.indexOf(']', nextOpenBracket + 1);
      if (nextCloseBracket == -1) {
        throw new IllegalArgumentException("Invalid remaining key: " + key);
      }
      String fieldName = key.substring(0, nextOpenBracket);
      int index = Integer.valueOf(key.substring(nextOpenBracket + 1, nextCloseBracket));
      String remainingKey = key.substring(nextCloseBracket + 1);
      ArrayNode arrayNode = (ArrayNode) node.get(fieldName);
      if (arrayNode == null) {
        arrayNode = node.putArray(fieldName);
      }
      if (remainingKey.isEmpty()) {
        arrayNode.add(value);
      } else if (remainingKey.startsWith(".")) {
        remainingKey = remainingKey.substring(1);
        ObjectNode childNode = (ObjectNode) arrayNode.get(index);
        if (childNode == null) {
          childNode = arrayNode.addObject();
        }
        populate(childNode, remainingKey, value);
      } else {
        throw new IllegalArgumentException("Unknown key format: " + key);
      }
    } else {
      node.set(key, TextNode.valueOf(value));
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
}
