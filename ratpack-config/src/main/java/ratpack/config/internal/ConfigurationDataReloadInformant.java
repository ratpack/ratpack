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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.server.ReloadInformant;

public class ConfigurationDataReloadInformant implements ReloadInformant {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationDataReloadInformant.class);

  private ObjectNode currentNode;
  private final ConfigurationDataLoader loader;

  public ConfigurationDataReloadInformant(ObjectNode currentNode, ConfigurationDataLoader loader) {
    this.currentNode = currentNode;
    this.loader = loader;
  }

  @Override
  public boolean shouldReload() {
    // TODO: load async/periodically
    ObjectNode newNode = loader.load();
    boolean changed = !currentNode.equals(newNode);
    LOGGER.debug("Comparing {} and {}; changed={}", currentNode, newNode, changed);
    return changed;
  }

  @Override
  public String toString() {
    return "configuration data reload informant";
  }
}
