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
import ratpack.exec.ExecControl;
import ratpack.server.ReloadInformant;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConfigDataReloadInformant implements ReloadInformant {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigDataReloadInformant.class);

  private final ObjectNode currentNode;
  private final AtomicBoolean changeDetected = new AtomicBoolean();
  private final ConfigDataLoader loader;
  private final Lock lock = new ReentrantLock();
  private Duration interval = Duration.ofSeconds(1);
  private ScheduledFuture<?> future;

  public ConfigDataReloadInformant(ObjectNode currentNode, ConfigDataLoader loader) {
    this.currentNode = currentNode;
    this.loader = loader;
  }

  public ConfigDataReloadInformant interval(Duration interval) {
    this.interval = interval;
    return this;
  }

  @Override
  public boolean shouldReload() {
    schedulePollIfNotRunning();
    return changeDetected.get();
  }

  private void schedulePollIfNotRunning() {
    if (!isPollRunning()) {
      lock.lock();
      try {
        if (!isPollRunning()) {
          future = schedulePoll();
        }
      } finally {
        lock.unlock();
      }
    }
  }

  private boolean isPollRunning() {
    return future != null && !future.isDone();
  }

  private ScheduledFuture<?> schedulePoll() {
    LOGGER.debug("Scheduling configuration poll in {}", interval);
    ExecControl execControl = ExecControl.current();
    ScheduledExecutorService scheduledExecutorService = execControl.getController().getExecutor();

    Runnable poll = () ->
      execControl.exec().start(e -> e
          .blocking(loader::load)
          .then(newNode -> {
            if (currentNode.equals(newNode)) {
              LOGGER.debug("No difference in configuration data");
              lock.lock();
              try {
                future = schedulePoll();
              } finally {
                lock.unlock();
              }
            } else {
              LOGGER.info("Configuration data difference detected; next request should reload");
              changeDetected.set(true);
            }
          })
      );

    return scheduledExecutorService.schedule(poll, interval.getSeconds(), TimeUnit.SECONDS);
  }

  @Override
  public String toString() {
    return "configuration data reload informant";
  }
}
