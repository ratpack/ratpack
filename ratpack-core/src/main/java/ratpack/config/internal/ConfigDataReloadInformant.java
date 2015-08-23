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
import ratpack.exec.Blocking;
import ratpack.exec.ExecController;
import ratpack.exec.Execution;
import ratpack.registry.Registry;
import ratpack.server.ReloadInformant;
import ratpack.server.Service;
import ratpack.server.StartEvent;
import ratpack.server.StopEvent;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ConfigDataReloadInformant implements ReloadInformant, Service {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigDataReloadInformant.class);
  private static final Duration INTERVAL = Duration.ofSeconds(1);

  private final ConfigDataLoader loader;
  private final ObjectNode currentNode;

  private boolean changeDetected;
  private boolean stopped;

  public ConfigDataReloadInformant(ObjectNode currentNode, ConfigDataLoader loader) {
    this.currentNode = currentNode;
    this.loader = loader;
  }

  @Override
  public boolean shouldReload(Registry registry) {
    return changeDetected;
  }

  private void schedulePoll() {
    if (shouldStop()) {
      return;
    }

    final ExecController controller = Execution.current().getController();
    ScheduledExecutorService scheduledExecutorService = controller.getExecutor();

    Runnable poll = () ->
      controller.fork().start(e -> {
          if (shouldStop()) {
            return;
          }
          Blocking.get(loader::load)
            .onError(error -> {
              LOGGER.warn("failed to load config in order to check for changes", error);
              schedulePoll();
            })
            .then(newNode -> {
              if (currentNode.equals(newNode)) {
                LOGGER.debug("No difference in configuration data");
                schedulePoll();
              } else {
                LOGGER.info("Configuration data difference detected; next request should reload");
                changeDetected = true;
              }
            });
        }
      );

    scheduledExecutorService.schedule(poll, INTERVAL.getSeconds(), TimeUnit.SECONDS);
  }

  private boolean shouldStop() {
    return stopped || Execution.current().getController().getEventLoopGroup().isShuttingDown();
  }

  @Override
  public String toString() {
    return "configuration data reload informant";
  }

  @Override
  public void onStart(StartEvent event) throws Exception {
    schedulePoll();
  }

  @Override
  public void onStop(StopEvent event) throws Exception {
    stopped = true;
  }
}
