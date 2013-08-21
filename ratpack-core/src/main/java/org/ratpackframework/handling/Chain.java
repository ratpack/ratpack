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
import org.ratpackframework.util.Action;

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
  Chain handler(Handler handler);

  /**
   * Add a prefix handler to the chain being constructed for the given prefix.
   * See {@link org.ratpackframework.handling.Handlers#prefix(String, org.ratpackframework.util.Action)} for format details on the prefix string.
   *
   * @param prefix The prefix to bind to
   * @param handlers The definition of the nested handlers
   */
  Chain prefix(String prefix, Handler... handlers);

  Chain prefix(String prefix, List<Handler> handlers);

  Chain prefix(String prefix, Action<? super Chain> chainAction);

  /**
   * Add a path handler to the chain being constructed for the given path.
   * <p>See also {@link org.ratpackframework.handling.Handlers#path(String, Handler)}
   *
   * @param path The path to match requests for, the match must be an exact match
   * @param handler The handler to delegate to if the request matches the given path exactly
   */
  Chain path(String path, Handler handler);

  /**
   * Add a GET handler to the chain being constructed for the given path.
   * <p>See also {@link org.ratpackframework.handling.Handlers#get(String, Handler)}
   *
   *
   * @param path The path to match requests for
   * @param handler The handler to delegate to if the path matches and the request is a GET
   * @return A Handler
   */
  Chain get(String path, Handler handler);

  /**
   * Add a GET handler to the chain being constructed for the root path.
   * <p>See also {@link org.ratpackframework.handling.Handlers#get(Handler)}
   *
   *
   * @param handler The handler to delegate to for the root path if the request is a GET
   * @return A Handler
   */
  Chain get(Handler handler);

  /**
   * Add a POST handler to the chain being constructed for the given path.
   * <p>See also {@link org.ratpackframework.handling.Handlers#post(String, Handler)}
   *
   * @param path The path to match requests for
   * @param handler The handler to delegate to if the path matches and the request is a POST
   */
  Chain post(String path, Handler handler);

  /**
   * Add a POST handler to the chain being constructed for the root path.
   * <p>See also {@link org.ratpackframework.handling.Handlers#post(Handler)}
   *
   * @param handler The handler to delegate to for the root path if the request is a POST
   */
  Chain post(Handler handler);

  /**
   * Add an asset handler to the chain being constructed.
   * <p>See also {@link org.ratpackframework.handling.Handlers#assets(String, String...)}
   *
   * @param path The relative path to the location of the assets to serve
   * @param indexFiles The index files to try if the request is for a directory
   */
  Chain assets(String path, String... indexFiles);

  /**
   * Adds a register handler to the chain being constructed, with the given service addition.
   * <p>See also {@link org.ratpackframework.handling.Handlers#register(Object, java.util.List)}
   *
   *
   * @param object The object to add to the service
   * @param handlers The handlers to register the service with
   * @return A Handler
   */
  Chain register(Object object, List<Handler> handlers);

  /**
   * Adds a register handler to the chain being constructed.
   * <p>See also {@link org.ratpackframework.handling.Handlers#register(Class, Object, java.util.List)}
   *
   *
   * @param type The type by which to make the service addition available
   * @param object The object to add to the service
   * @param handlers The handlers to register the service with
   * @return A Handler
   */
  <T> Chain register(Class<? super T> type, T object, List<Handler> handlers);

  /**
   * Adds a filesystem handler to the chain being constructed.
   * <p>See also {@link org.ratpackframework.handling.Handlers#fileSystem(String, java.util.List)}
   *
   * @param path The relative path to the new file system binding point
   * @param handlers The definition of the handler chain
   */
  Chain fileSystem(String path, List<Handler> handlers);

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
