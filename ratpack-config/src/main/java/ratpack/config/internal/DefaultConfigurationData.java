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

package ratpack.config.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.google.common.collect.ImmutableList;
import ratpack.config.ConfigurationData;
import ratpack.config.ConfigurationSource;
import ratpack.util.ExceptionUtils;

import java.io.IOException;
import java.util.Iterator;

public class DefaultConfigurationData implements ConfigurationData {
  private final ObjectMapper objectMapper;
  private final ObjectNode rootNode;

  public DefaultConfigurationData(ObjectMapper objectMapper, ImmutableList<ConfigurationSource> configurationSources) {
    this.objectMapper = objectMapper;
    rootNode = objectMapper.createObjectNode();
    try {
      for (ConfigurationSource source : configurationSources) {
        merge(source.loadConfigurationData(objectMapper), rootNode);
      }
    } catch (Exception ex) {
      throw ExceptionUtils.uncheck(ex);
    }
  }

  @Override
  public <O> O get(String pointer, Class<O> type) {
    ObjectNode curNode = pointer != null ? (ObjectNode) rootNode.at(pointer) : rootNode;
    try {
      return objectMapper.readValue(new TreeTraversingParser(curNode, objectMapper), type);
    } catch (IOException ex) {
      throw ExceptionUtils.uncheck(ex);
    }
  }

  /**
   * Merges node data from the source into the dest, overwriting non-object fields in dest if they already exist.
   */
  private void merge(JsonNode sourceNode, JsonNode destNode) {
    Iterator<String> fieldNames = sourceNode.fieldNames();
    while (fieldNames.hasNext()) {
      String fieldName = fieldNames.next();
      JsonNode sourceFieldValue = sourceNode.get(fieldName);
      JsonNode destFieldValue = destNode.get(fieldName);
      if (destFieldValue != null && destFieldValue.isObject()) {
        merge(sourceFieldValue, destFieldValue);
      } else if (destNode instanceof ObjectNode) {
        ((ObjectNode) destNode).replace(fieldName, sourceFieldValue);
      }
    }
  }
}
