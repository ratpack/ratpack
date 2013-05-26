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

import org.ratpackframework.api.NonBlocking;

/**
 * A handler participates in the processing of a request/response pair (i.e. an {@link Exchange}).
 * <p>
 * Handlers do not necessarily generate a response to the request, though they can.
 * They may delegate to, or cooperate with other handlers.
 * <p>
 * Handlers can do one of four things:
 * <ol>
 * <li>Send a response back to the client, terminating processing</li>
 * <li>Opt out of processing the exchange, passing control to the next handler in the pipeline via {@link org.ratpackframework.handling.Exchange#next()}</li>
 * <li>Insert handlers into the pipeline via {@link Exchange#insert(org.ratpackframework.context.Context, Handler...)} (or related methods)</li>
 * <li>Forward the exchange to another internal handler by calling its {@link #handle(Exchange)} method</li>
 * </ol>
 */
public interface Handler {

  /**
   * Handles the exchange.
   *
   * @param exchange The exchange to handle
   * @see Handler
   */
  @NonBlocking
  void handle(Exchange exchange);

}
