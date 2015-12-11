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

package ratpack.error;

import com.google.common.reflect.TypeToken;
import ratpack.handling.Context;
import ratpack.util.Types;

/**
 * The client error handler deals with errors that are due to the client doing something wrong.
 * <p>
 * Examples:
 * <ul>
 *   <li>Unsupported media type (415)
 *   <li>Unsupported method (405)
 * </ul>
 */
public interface ClientErrorHandler {

  /**
   * A type token for this type.
   *
   * @since 1.1
   */
  TypeToken<ClientErrorHandler> TYPE = Types.token(ClientErrorHandler.class);

  /**
   * Handle a client error.
   *
   * @param context The context
   * @param statusCode The 4xx status code that explains the problem
   * @throws Exception if a problem occurs reacting to the client error (will be forwarded to the server error handler)
   */
  void error(Context context, int statusCode) throws Exception;

}
