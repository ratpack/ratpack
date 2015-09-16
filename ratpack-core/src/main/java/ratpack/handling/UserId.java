/*
 * Copyright 2015 the original author or authors.
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

package ratpack.handling;

import ratpack.handling.internal.DefaultUserId;

/**
 * An opaque identifier for the “user” that initiated the request.
 * <p>
 * This type is typically used in logging, notably by {@link RequestLogger}.
 * Authentication systems should add a user identifier to the request registry.
 */
public interface UserId extends CharSequence {

  /**
   * Creates new user identifier of the given string.
   *
   * @param userIdentifier the user identifier
   * @return a user identifier object
   */
  static UserId of(String userIdentifier) {
    return new DefaultUserId(userIdentifier);
  }

}
