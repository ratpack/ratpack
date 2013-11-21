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

package ratpack.http;

/**
 * The method of a HTTP request.
 */
@SuppressWarnings("UnusedDeclaration")
public interface HttpMethod {

  /**
   * The name of the method, always in upper case.
   *
   * @return The name of the method, always in upper case.
   */
  String getName();

  /**
   * True if the method is POST.
   *
   * @return True if the method is POST.
   */
  boolean isPost();

  /**
   * True if the method is GET or HEAD.
   *
   * @return True if the method is GET or HEAD.
   */
  boolean isGet();

  /**
   * True if the method is PUT.
   *
   * @return True if the method is PUT.
   */
  boolean isPut();

  /**
   * True if the method is DELETE.
   *
   * @return True if the method is DELETE.
   */
  boolean isDelete();

  /**
   * True if the method is OPTIONS.
   *
   * @return True if the method is OPTIONS.
   */
  boolean isOptions();

  /**
   * True if the method is HEAD.
   *
   * @return True if the method is HEAD.
   */
  boolean isHead();

  /**
   * Returns true if the method has the given name, insensitive to case.
   *
   * @param name The name of the method to compare to the actual method name.
   * @return True if the given name equals {@link #getName()} irrespective of case.
   */
  boolean name(String name);

}
