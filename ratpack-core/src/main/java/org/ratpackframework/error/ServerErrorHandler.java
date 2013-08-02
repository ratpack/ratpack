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

package org.ratpackframework.error;

import org.ratpackframework.api.NonBlocking;
import org.ratpackframework.handling.Context;

/**
 * An object that can deal with errors that occur during the processing of an exchange.
 *
 * Typically retrieved from the exchange service.
 *
 * @see org.ratpackframework.handling.Context#error(Exception)
 * @see org.ratpackframework.handling.Context#withErrorHandling(Runnable)
 */
public interface ServerErrorHandler {

  /**
   * Processes the given exception that occurred processing the given context.
   *
   * @param context The context being processed
   * @param exception The exception that occurred
   */
  @NonBlocking
  void error(Context context, Exception exception);

}
