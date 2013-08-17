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

package org.ratpackframework.handling;

import org.ratpackframework.api.Nullable;
import org.ratpackframework.registry.Registry;

import java.util.List;

/**
 * A chain can be used to build a linked series of handlers.
 * <p>
 * The {@code Chain} type does not represent the handlers "in action".
 * That is, it is the construction of a handler chain.
 *
 * @see Handlers#chain(org.ratpackframework.util.Action)
 */
public interface Chain {

  /**
   * Add the given handler to the chain being constructed.
   *
   * @param handler The handler to add to the chain being constructed
   */
  void add(Handler handler);

  /**
   * Add the given handler to the chain being constructed.
   *
   * @param handler The handler to add to the chain being constructed
   */
  void handler(Handler handler);

  /**
   * Add a prefix handler to the chain being constructed for the given prefix.
   * See {@link org.ratpackframework.handling.Handlers#prefix(String, org.ratpackframework.util.Action)} for format details on the prefix string.
   *
   * @param prefix The prefix to bind to
   * @param handlers The definition of the nested handlers
   */
  void prefix(String prefix, List<Handler> handlers);

  /**
   * Add a path handler to the chain being constructed for the given path.
   *
   * @param path The path to match requests for, the match must be an exact match
   * @param handler The handler to delegate to if the request matches the given path exactly
   */
  void path(String path, Handler handler);

  /**
   * Add a GET handler to the chain being constructed for the given path.
   *
   * @param path The path to match requests for
   * @param handler The handler to delegate to if the path matches and the request is a GET
   */
  void get(String path, Handler handler);

  /**
   * Add a GET handler to the chain being constructed for the root path.
   *
   * @param handler The handler to delegate to for the root path if the request is a GET
   */
  void get(Handler handler);

  /**
   * Add a POST handler to the chain being constructed for the given path.
   *
   * @param path The path to match requests for
   * @param handler The handler to delegate to if the path matches and the request is a POST
   */
  void post(String path, Handler handler);

  /**
   * Add a POST handler to the chain being constructed for the root path.
   *
   * @param handler The handler to delegate to for the root path if the request is a POST
   */
  void post(Handler handler);

  /**
   * Adds a register handler to the chain being constructed, with the given service addition.
   *
   * @param object The object to add to the service
   * @param handlers The handlers to register the service with
   */
  void register(Object object, List<Handler> handlers);

  /**
   * Adds a register handler to the chain being constructed.
   *
   * @param type The type by which to make the service addition available
   * @param object The object to add to the service
   * @param handlers The handlers to register the service with
   * @param <T> The concrete type of the service addition
   */
  <T> void register(Class<? super T> type, T object, List<Handler> handlers);

  void fileSystem(String path, List<Handler> handlers);

  /**
   * Add an asset handler to the chain being constructed.
   *
   * @param path The relative path to the location of the assets to serve
   * @param indexFiles The index files to try if the request is for a directory
   */
  void assets(String path, String[] indexFiles);

  /**
   * The registry that backs this chain.
   * <p>
   * The registry that is available is dependent on how the chain was constructed.
   *
   * @return The registry that backs this chain, or {@code null} if this chain has no registry.
   */
  @Nullable
  Registry<Object> getRegistry();

}
