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

package ratpack.event.internal;

import ratpack.func.Action;

import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultEventController<T> implements EventController<T> {

  private final static Logger LOGGER = LoggerFactory.getLogger(DefaultEventController.class);

  private final List<Action<? super T>> handlers = new LinkedList<>();

  private boolean fired;

  @Override
  public EventRegistry<T> getRegistry() {
    return new EventRegistry<T>() {
      @Override
      public void register(Action<? super T> eventHandler) {
        if (fired) {
          LOGGER.warn("Cannot register event listener as event has been fired: " + eventHandler, new Exception());
        } else {
          handlers.add(eventHandler);
        }
      }
    };
  }

  @Override
  public void fire(T payload) {
    if (fired) {
      LOGGER.warn("Cannot fire event with payload as event has been fired: " + payload, new Exception());
    } else {
      fired = true;
      for (Action<? super T> handler : handlers) {
        try {
          handler.execute(payload);
        } catch (Exception e) {
          LOGGER.warn("Ignoring exception thrown by event handler when receiving payload: " + payload, e);
        }
      }
    }
  }

  public boolean isHasListeners() {
    return !handlers.isEmpty();
  }

}
