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
import ratpack.config.ConfigSource;
import ratpack.file.FileSystemBinding;
import ratpack.util.Exceptions;

import java.util.Iterator;

public class ConfigDataLoader {
  private final ObjectMapper objectMapper;
  private final Iterable<ConfigSource> configSources;
  private final FileSystemBinding fileSystemBinding;

  public ConfigDataLoader(ObjectMapper objectMapper, Iterable<ConfigSource> configSources, FileSystemBinding fileSystemBinding) {
    this.objectMapper = objectMapper;
    this.configSources = configSources;
    this.fileSystemBinding = fileSystemBinding;
  }

  public ObjectNode load() {
    ObjectNode node = objectMapper.createObjectNode();
    try {
      for (ConfigSource source : configSources) {
        merge(source.loadConfigData(objectMapper, fileSystemBinding), node);
      }
    } catch (Exception ex) {
      throw Exceptions.uncheck(ex);
    }
    return node;
  }

  /**
   * Merges node data from the source into the dest, overwriting non-object fields in dest if they already exist.
   */
  private void merge(JsonNode sourceNode, JsonNode destNode) {
    if (sourceNode != null) {
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
}
