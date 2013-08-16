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
   * Add a GET handler to the chain being constructed for the given path.
   * @param path The path to match requests for
   * @param handler The handler to delegate to if the path matches and the request is a GET
   */
  void get(String path, Handler handler);

  /**
   * Add a GET handler to the chain being constructed for the root path.
   * @param handler The handler to delegate to for the root path if the request is a GET
   */
  void get(Handler handler);

  /**
   * Add a POST handler to the chain being constructed for the given path.
   * @param path The path to match requests for
   * @param handler The handler to delegate to if the path matches and the request is a POST
   */
  void post(String path, Handler handler);

  /**
   * Add a POST handler to the chain being constructed for the root path.
   * @param handler The handler to delegate to for the root path if the request is a POST
   */
  void post(Handler handler);

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
