/*
 * Copyright 2017 the original author or authors.
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

package ratpack.http;

import ratpack.handling.Context;

/**
 * Indicates that this exception represents a client error.
 * If thrown within a handler, will result in {@link Context#clientError(int)} being called,
 * instead of the exception propagating.
 *
 * @since 1.5
 */
public abstract class ClientErrorException extends RuntimeException {

  public ClientErrorException(String message) {
    super(message);
  }

  public ClientErrorException(String message, Throwable cause) {
    super(message, cause);
  }

  public abstract int getClientErrorCode();

}
