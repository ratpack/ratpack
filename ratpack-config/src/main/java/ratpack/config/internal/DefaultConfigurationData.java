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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.google.common.collect.ImmutableList;
import ratpack.config.ConfigurationData;
import ratpack.config.ConfigurationSource;
import ratpack.server.ReloadInformant;
import ratpack.util.ExceptionUtils;

import java.io.IOException;

public class DefaultConfigurationData implements ConfigurationData {
  private final ObjectMapper objectMapper;
  private final ObjectNode rootNode;
  private final ReloadInformant reloadInformant;

  public DefaultConfigurationData(ObjectMapper objectMapper, ImmutableList<ConfigurationSource> configurationSources) {
    ConfigurationDataLoader loader = new ConfigurationDataLoader(objectMapper, configurationSources);
    this.objectMapper = objectMapper;
    this.rootNode = loader.load();
    this.reloadInformant = new ConfigurationDataReloadInformant(rootNode, loader);
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

  @Override
  public ReloadInformant getReloadInformant() {
    return reloadInformant;
  }
}
