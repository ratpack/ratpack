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

package org.ratpackframework.context;

/**
 * Thrown when a request is made for an object that a context cannot provide.
 *
 * @see Context#get(Class)
 */
public class NotInContextException extends RuntimeException {

  /**
   * Constructs the exception.
   *
   * @param context The context that the object was requested of
   * @param type The requested type of the object
   */
  public NotInContextException(Context context, Class<?> type) {
    super(String.format("No object for type '%s' in context: %s", type.getName(), context.toString()));
  }

}
