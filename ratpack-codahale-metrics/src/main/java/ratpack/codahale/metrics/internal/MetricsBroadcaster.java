/*
 * Copyright 2013 the original author or authors.
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

package ratpack.codahale.metrics.internal;

import ratpack.func.Action;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A message broadcaster for sending metrics to its subscribers.
 */
public class MetricsBroadcaster {

  private final static Logger LOGGER = LoggerFactory.getLogger(MetricsBroadcaster.class);
  private final List<Action<String>> listeners = new CopyOnWriteArrayList<>();

  public AutoCloseable register(final Action<String> subscriber) {
    listeners.add(subscriber);

    return new AutoCloseable() {
      @Override
      public void close() {
        listeners.remove(subscriber);
      }
    };
  }

  public void broadcast(String message) {
    for (Action<String> listener : listeners) {
      try {
        listener.execute(message);
      } catch (Exception e) {
        LOGGER.warn("Exception encountered while broadcasting metrics: " + e.getLocalizedMessage());
      }
    }
  }

  public boolean hasListeners() {
    return !listeners.isEmpty();
  }

}
