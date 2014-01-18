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

package ratpack.handling.internal;

import ratpack.handling.Context;
import ratpack.handling.Handler;

import static ratpack.registry.Registries.registry;

public class RegisteringHandler implements Handler {

  private final Class<?> type;
  private final Object object;
  private final Handler handler;

  @SuppressWarnings("unchecked")
  public <T> RegisteringHandler(T object, Handler handler) {
    this.type = null;
    this.object = object;
    this.handler = handler;
  }

  public <T> RegisteringHandler(Class<? super T> type, T object, Handler handler) {
    this.type = type;
    this.object = object;
    this.handler = handler;
  }

  @SuppressWarnings("unchecked")
  public void handle(Context context) {
    if (type == null) {
      context.insert(registry(object), handler);
    } else {
      context.insert(registry((Class) type, object), handler);
    }
  }
}
