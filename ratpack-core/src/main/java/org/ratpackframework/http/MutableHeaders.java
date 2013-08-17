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

package org.ratpackframework.http;

public interface MutableHeaders extends Headers {

  /**
   * Adds a new header with the specified name and value.
   * <p>
   * Will not replace any existing values for the header.
   *
   * @param name The name of the header
   * @param value The value of the header
   */
  void add(String name, Object value);

  /**
   * Sets the (only) value for the header with the specified name.
   * <p>
   * All existing values for the same header will be removed.
   *
   * @param name The name of the header
   * @param value The value of the header
   */
  void set(String name, Object value);

  /**
   * Sets a new header with the specified name and values.
   * <p>
   * All existing values for the same header will be removed.
   *
   * @param name The name of the header
   * @param values The values of the header
   */
  void set(String name, Iterable<?> values);

  /**
   * Removes the header with the specified name.
   *
   * @param name The name of the header to remove.
   */
  void remove(String name);

  /**
   * Removes all headers from this message.
   */
  void clear();

}
