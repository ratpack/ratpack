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
import ratpack.config.ConfigData;
import ratpack.config.ConfigDataSpec;
import ratpack.config.ConfigObject;
import ratpack.registry.Registry;
import ratpack.server.ServerConfig;
import ratpack.server.StartEvent;
import ratpack.server.StopEvent;
import ratpack.util.Exceptions;

import java.io.IOException;

public class DefaultConfigData implements ConfigData {

  private final ObjectMapper objectMapper;
  private final ObjectNode rootNode;
  private final ConfigDataReloadInformant reloadInformant;
  private final ObjectNode emptyNode;

  public DefaultConfigData(ConfigDataSpec configDataSpec) {
    this.objectMapper = configDataSpec.getObjectMapper();
    ConfigDataLoader loader = new ConfigDataLoader(objectMapper, configDataSpec.getConfigSources());
    this.rootNode = loader.load();
    this.reloadInformant = new ConfigDataReloadInformant(rootNode, loader);
    this.emptyNode = objectMapper.getNodeFactory().objectNode();
  }

  @Override
  public <O> ConfigObject<O> getAsConfigObject(String pointer, Class<O> type) {
    JsonNode node = pointer != null ? rootNode.at(pointer) : rootNode;
    if (node.isMissingNode()) {
      node = emptyNode;
    }
    try {
      O value = objectMapper.readValue(new TreeTraversingParser(node, objectMapper), type);
      return new DefaultConfigObject<>(pointer, type, value);
    } catch (IOException ex) {
      throw Exceptions.uncheck(ex);
    }
  }

  @Override
  public boolean shouldReload(Registry registry) {
    return reloadInformant.shouldReload(registry);
  }

  @Override
  public void onStart(StartEvent event) throws Exception {
    ServerConfig serverConfig = event.getRegistry().get(ServerConfig.class);
    if (serverConfig.isDevelopment()) {
      reloadInformant.onStart(event);
    }
  }

  @Override
  public void onStop(StopEvent event) throws Exception {
    ServerConfig serverConfig = event.getRegistry().get(ServerConfig.class);
    if (serverConfig.isDevelopment()) {
      reloadInformant.onStop(event);
    }
  }
}
