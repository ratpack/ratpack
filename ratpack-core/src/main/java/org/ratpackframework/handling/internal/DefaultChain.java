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

import java.util.List;

public class DefaultChain implements Chain {

  private final List<Handler> handlers;
  private final Registry<Object> registry;

  public DefaultChain(List<Handler> handlers, @Nullable Registry<Object> registry) {
    this.handlers = handlers;
    this.registry = registry;
  }

  public void add(Handler handler) {
    handlers.add(handler);
  }

  public void handler(Handler handler) {
    add(handler);
  }

  public void prefix(String prefix, List<Handler> handlers) {
    add(Handlers.prefix(prefix, handlers));
  }

  public void path(String path, Handler handler) {
    add(Handlers.path(path, handler));
  }

  public Handler get(String path, Handler handler) {
    Handler getHandler = Handlers.get(path, handler);
    add(getHandler);
    return getHandler;
  }

  public Handler get(Handler handler) {
    return get("", handler);
  }

  public void post(String path, Handler handler) {
    add(Handlers.post(path, handler));
  }

  public void post(Handler handler) {
    post("", handler);
  }

  public void assets(String path, String[] indexFiles) {
    add(Handlers.assets(path, indexFiles));
  }

  public void assets(String path, Handler notFound) {
    add(Handlers.assets(path, notFound));
  }

  public Handler register(Object object, List<Handler> handlers) {
    Handler registerHandler = Handlers.register(object, handlers);
    add(registerHandler);
    return registerHandler;
  }

  public <T> Handler register(Class<? super T> type, T object, List<Handler> handlers) {
    Handler registerHandler = Handlers.register(type, object, handlers);
    add(registerHandler);
    return registerHandler;
  }

  public void fileSystem(String path, List<Handler> handlers) {
    add(Handlers.fileSystem(path, handlers));
  }

  public Registry<Object> getRegistry() {
    return registry;
  }
}
