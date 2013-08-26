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

package org.ratpackframework.handling.internal;

import org.ratpackframework.api.Nullable;
import org.ratpackframework.handling.Chain;
import org.ratpackframework.handling.Handler;
import org.ratpackframework.handling.Handlers;
import org.ratpackframework.registry.Registry;
import org.ratpackframework.util.Action;

import java.util.List;

public class DefaultChain implements Chain {

  private final List<Handler> handlers;
  private final Registry<Object> registry;

  public DefaultChain(List<Handler> handlers, @Nullable Registry<Object> registry) {
    this.handlers = handlers;
    this.registry = registry;
  }

  public Chain handler(Handler handler) {
    handlers.add(handler);
    return this;
  }

  public Chain prefix(String prefix, Handler... handlers) {
    return handler(Handlers.prefix(prefix, handlers));
  }

  public Chain prefix(String prefix, List<Handler> handlers) {
    return handler(Handlers.prefix(prefix, handlers));
  }

  public Chain prefix(String prefix, Action<? super Chain> builder) {
    return handler(Handlers.prefix(prefix, builder));
  }

  public Chain handler(String path, Handler handler) {
    return handler(Handlers.path(path, handler));
  }

  public Chain get(String path, Handler handler) {
    return handler(Handlers.get(path, handler));
  }

  public Chain get(Handler handler) {
    return get("", handler);
  }

  public Chain post(String path, Handler handler) {
    return handler(Handlers.post(path, handler));
  }

  public Chain post(Handler handler) {
    return post("", handler);
  }

  public Chain put(String path, Handler handler) {
    return handler(Handlers.put(path, handler));
  }

  public Chain put(Handler handler) {
    return put("", handler);
  }

  public Chain delete(String path, Handler handler) {
    return handler(Handlers.delete(path, handler));
  }

  public Chain delete(Handler handler) {
    return delete("", handler);
  }

  public Chain assets(String path, String... indexFiles) {
    return handler(Handlers.assets(path, indexFiles));
  }

  public Chain register(Object service, List<Handler> handlers) {
    return handler(Handlers.register(service, handlers));
  }

  public <T> Chain register(Class<? super T> type, T service, List<Handler> handlers) {
    return handler(Handlers.register(type, service, handlers));
  }

  public Chain fileSystem(String path, List<Handler> handlers) {
    return handler(Handlers.fileSystem(path, handlers));
  }

  public Registry<Object> getRegistry() {
    return registry;
  }
}
