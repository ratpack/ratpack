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

import ratpack.api.Nullable;
import ratpack.func.Action;
import ratpack.handling.Chain;
import ratpack.handling.Handler;
import ratpack.handling.Handlers;
import ratpack.registry.Registries;
import ratpack.registry.Registry;
import ratpack.registry.RegistrySpec;
import ratpack.server.ServerConfig;

import java.util.List;

import static com.google.common.collect.ImmutableList.copyOf;

public class DefaultChain implements Chain {

  private final List<Handler> handlers;
  private final ServerConfig serverConfig;
  private final Registry registry;

  public DefaultChain(List<Handler> handlers, ServerConfig serverConfig, @Nullable Registry registry) {
    this.handlers = handlers;
    this.serverConfig = serverConfig;
    this.registry = registry;
  }

  @Override
  public Handler chain(Action<? super Chain> action) throws Exception {
    return Handlers.chain(getServerConfig(), getRegistry(), action);
  }

  public Chain delete(String path, Handler handler) {
    return all(Handlers.path(path, Handlers.chain(Handlers.delete(), handler)));
  }

  public Chain delete(Handler handler) {
    return delete("", handler);
  }

  public Chain fileSystem(String path, Action<? super Chain> action) throws Exception {
    return all(Handlers.fileSystem(getServerConfig(), path, chain(action)));
  }

  public Chain get(String path, Handler handler) {
    return all(Handlers.path(path, Handlers.chain(Handlers.get(), handler)));
  }

  public Chain get(Handler handler) {
    return get("", handler);
  }

  public ServerConfig getServerConfig() {
    return serverConfig;
  }

  public Registry getRegistry() {
    return registry;
  }

  public Chain all(Handler handler) {
    handlers.add(handler);
    return this;
  }

  public Chain path(String path, Handler handler) {
    return all(Handlers.path(path, handler));
  }

  public Chain header(String headerName, String headerValue, Handler handler) {
    return all(Handlers.header(headerName, headerValue, handler));
  }

  public Chain host(String hostName, Action<? super Chain> action) throws Exception {
    return all(Handlers.host(hostName, chain(action)));
  }

  public Chain patch(String path, Handler handler) {
    return all(Handlers.path(path, Handlers.chain(Handlers.patch(), handler)));
  }

  public Chain patch(Handler handler) {
    return patch("", handler);
  }

  public Chain post(String path, Handler handler) {
    return all(Handlers.path(path, Handlers.chain(Handlers.post(), handler)));
  }

  public Chain post(Handler handler) {
    return post("", handler);
  }

  public Chain prefix(String prefix, Action<? super Chain> action) throws Exception {
    return all(Handlers.prefix(prefix, chain(action)));
  }

  public Chain put(String path, Handler handler) {
    return all(Handlers.path(path, Handlers.chain(Handlers.put(), handler)));
  }

  public Chain put(Handler handler) {
    return put("", handler);
  }

  @Override
  public Chain register(Registry registry) {
    return all(Handlers.register(registry));
  }

  @Override
  public Chain register(Action<? super RegistrySpec> action) throws Exception {
    return all(Handlers.register(Registries.registry(action)));
  }

  public Chain register(Action<? super RegistrySpec> registryAction, Action<? super Chain> chainAction) throws Exception {
    return register(Registries.registry(registryAction), chainAction);
  }

  @Override
  public Chain register(Registry registry, Action<? super Chain> action) throws Exception {
    return all(Handlers.register(registry, chain(action)));
  }

  @Override
  public Chain insert(Action<? super Chain> action) throws Exception {
    return all(chain(action));
  }

  @Override
  public Chain redirect(int code, String location) {
    return all(Handlers.redirect(code, location));
  }

}
