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
import ratpack.launch.LaunchConfig;
import ratpack.registry.Registries;
import ratpack.registry.Registry;
import ratpack.registry.RegistrySpec;

import java.util.List;

import static com.google.common.collect.ImmutableList.copyOf;

public class DefaultChain implements Chain {

  private final List<Handler> handlers;
  private final LaunchConfig launchConfig;
  private final Registry registry;

  public DefaultChain(List<Handler> handlers, LaunchConfig launchConfig, @Nullable Registry registry) {
    this.handlers = handlers;
    this.launchConfig = launchConfig;
    this.registry = registry;
  }

  public Chain assets(String path, String... indexFiles) {
    return handler(Handlers.assets(getLaunchConfig(), path, indexFiles.length == 0 ? launchConfig.getIndexFiles() : copyOf(indexFiles)));
  }

  @Override
  public Handler chain(Action<? super Chain> action) throws Exception {
    return Handlers.chain(getLaunchConfig(), getRegistry(), action);
  }

  public Chain delete(String path, Handler handler) {
    return handler(Handlers.path(path, Handlers.chain(Handlers.delete(), handler)));
  }

  public Chain delete(Handler handler) {
    return delete("", handler);
  }

  public Chain fileSystem(String path, Handler handler) {
    return handler(Handlers.fileSystem(getLaunchConfig(), path, handler));
  }

  public Chain fileSystem(String path, Action<? super Chain> action) throws Exception {
    return handler(Handlers.fileSystem(getLaunchConfig(), path, chain(action)));
  }

  public Chain get(String path, Handler handler) {
    return handler(Handlers.path(path, Handlers.chain(Handlers.get(), handler)));
  }

  public Chain get(Handler handler) {
    return get("", handler);
  }

  public LaunchConfig getLaunchConfig() {
    return launchConfig;
  }

  public Registry getRegistry() {
    return registry;
  }

  public Chain handler(Handler handler) {
    handlers.add(handler);
    return this;
  }

  public Chain handler(String path, Handler handler) {
    return handler(Handlers.path(path, handler));
  }

  public Chain header(String headerName, String headerValue, Handler handler) {
    return handler(Handlers.header(headerName, headerValue, handler));
  }

  public Chain patch(String path, Handler handler) {
    return handler(Handlers.path(path, Handlers.chain(Handlers.patch(), handler)));
  }

  public Chain patch(Handler handler) {
    return patch("", handler);
  }

  public Chain post(String path, Handler handler) {
    return handler(Handlers.path(path, Handlers.chain(Handlers.post(), handler)));
  }

  public Chain post(Handler handler) {
    return post("", handler);
  }

  public Chain prefix(String prefix, Handler handler) {
    return handler(Handlers.prefix(prefix, handler));
  }

  public Chain prefix(String prefix, Action<? super Chain> action) throws Exception {
    return prefix(prefix, chain(action));
  }

  public Chain put(String path, Handler handler) {
    return handler(Handlers.path(path, Handlers.chain(Handlers.put(), handler)));
  }

  public Chain put(Handler handler) {
    return put("", handler);
  }

  @Override
  public Chain register(Registry registry) {
    return handler(Handlers.register(registry));
  }

  @Override
  public Chain register(Action<? super RegistrySpec> action) throws Exception {
    return handler(Handlers.register(Registries.registry(action)));
  }

  public Chain register(Registry registry, Handler handler) {
    return handler(Handlers.register(registry, handler));
  }

  public Chain register(Action<? super RegistrySpec> registryAction, Action<? super Chain> chainAction) throws Exception {
    return register(Registries.registry(registryAction), chainAction);
  }

  @Override
  public Chain register(Registry registry, Action<? super Chain> action) throws Exception {
    return register(registry, chain(action));
  }

  @Override
  public Chain register(Action<? super RegistrySpec> registryAction, Handler handler) throws Exception {
    return register(Registries.registry(registryAction), handler);
  }

  @Override
  public Chain insert(Action<? super Chain> action) throws Exception {
    return handler(chain(action));
  }

}
