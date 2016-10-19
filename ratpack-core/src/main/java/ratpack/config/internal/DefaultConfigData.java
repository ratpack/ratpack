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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.google.common.reflect.TypeToken;
import ratpack.config.ConfigData;
import ratpack.config.ConfigObject;
import ratpack.config.ConfigSource;
import ratpack.file.FileSystemBinding;
import ratpack.util.Exceptions;

import java.io.IOException;

public class DefaultConfigData implements ConfigData {

  private final ObjectMapper objectMapper;
  private final ObjectNode rootNode;
  private final ObjectNode emptyNode;

  public DefaultConfigData(ObjectMapper objectMapper, Iterable<ConfigSource> configSources, FileSystemBinding fileSystemBinding) {
    this.objectMapper = objectMapper;
    ConfigDataLoader loader = new ConfigDataLoader(this.objectMapper, configSources, fileSystemBinding);
    this.rootNode = loader.load();
    this.emptyNode = this.objectMapper.getNodeFactory().objectNode();
  }

  @Override
  public ObjectNode getRootNode() {
    return this.rootNode;
  }

  @Override
  public <O> ConfigObject<O> getAsConfigObject(String pointer, TypeToken<O> type) {
    JsonNode node = pointer != null ? rootNode.at(pointer) : rootNode;
    if (node.isMissingNode()) {
      node = emptyNode;
    }
    try {
      JavaType javaType = objectMapper.getTypeFactory().constructType(type.getType());
      O value = objectMapper.readValue(new TreeTraversingParser(node, objectMapper), javaType);
      return new DefaultConfigObject<>(pointer, type, value);
    } catch (IOException ex) {
      throw Exceptions.uncheck(ex);
    }
  }

}
