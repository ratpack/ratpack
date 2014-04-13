/*
 * Copyright 2014 the original author or authors.
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

package ratpack.exec.internal;

import java.util.ArrayList;
import java.util.List;

public class FinishedOnThreadCallbackManager {

  private final ThreadLocal<List<Runnable>> callbacksHolder = new ThreadLocal<>();

  public void register(Runnable callback) {
    List<Runnable> callbacks = callbacksHolder.get();
    if (callbacks == null) {
      callbacks = new ArrayList<>(1);
      callbacksHolder.set(callbacks);
    }
    callbacks.add(callback);
  }

  public void fire() {
    List<Runnable> callbacks = callbacksHolder.get();
    if (callbacks != null) {
      callbacksHolder.remove();
      for (Runnable callback : callbacks) {
        callback.run();
      }
    }
  }


}
