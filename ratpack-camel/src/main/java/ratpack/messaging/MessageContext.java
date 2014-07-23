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

package ratpack.messaging;

import ratpack.registry.Registry;

/**
 * The context of an individual {@link ratpack.messaging.MessageHandler} invocation.
 *
 * It provides:
 * <ul>
 *   <li>Access to the {@link #getRequest() request} and {@link #getResponse() response} objects</li>
 *   <li>Delegation to a subsequent handler via the {@link #next()} method (handler order is prescribed by the registry implementation)</li>
 *   <li>Programmatic delegation via the {@link #insert(MessageHandler...)} family of methods</li>
 *   <li>Access to components in the registry via the {@link #get(Class)} method</li>
 * </ul>
 */
public interface MessageContext {
  /**
   * Returns this
   * @return this
   */
  MessageContext getContext();

  /**
   * Gets a component out of the registry
   *
   * @param type of the compoennt class
   * @return component implementation
   */
  <O> O get(Class<O> type);

  /**
   * Handle on the message request, including its headers.
   *
   * @return {@link ratpack.messaging.MessageRequest}
   */
  MessageRequest getRequest();

  /**
   * Handle on the message response, including its output stream.
   *
   * @return {@link ratpack.messaging.MessageResponse}
   */
  MessageResponse getResponse();

  /**
   * Hands the request off to the next handler in the chain
   */
  void next();

  /**
   * Programmatically inserts another handler into the processing chain
   * @param handlers
   */
  void insert(MessageHandler... handlers);

  /**
   * Programmatically inserts another handler into the processing chain with a specific registry implementation
   * @param registry
   * @param handlers
   */
  void insert(Registry registry, MessageHandler... handlers);
}
