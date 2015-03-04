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

package ratpack.handling;

import ratpack.func.NoArgAction;

/**
 * A specification of how to respond to a request, based on the requested method.
 *
 * @see Context#byMethod(ratpack.func.Action)
 */
public interface ByMethodSpec {

  /**
   * Defines the action to to take if the request has a HTTP method of GET.
   *
   * @param handler The handler to invoke if the request method matches
   * @return this
   */
  ByMethodSpec get(NoArgAction handler);

  /**
   * Defines the action to to take if the request has a HTTP method of GET.
   *
   * @param handler The handler to invoke if the request method matches
   * @return this
   */
  ByMethodSpec get(Handler handler);

  /**
   * Defines the action to to take if the request has a HTTP method of POST.
   *
   * @param handler The handler to invoke if the request method matches
   * @return this
   */
  ByMethodSpec post(NoArgAction handler);

  /**
   * Defines the action to to take if the request has a HTTP method of POST.
   *
   * @param handler The handler to invoke if the request method matches
   * @return this
   */
  ByMethodSpec post(Handler handler);

  /**
   * Defines the action to to take if the request has a HTTP method of PUT.
   *
   * @param handler The handler to invoke if the request method matches
   * @return this
   */
  ByMethodSpec put(NoArgAction handler);

  /**
   * Defines the action to to take if the request has a HTTP method of PUT.
   *
   * @param handler The handler to invoke if the request method matches
   * @return this
   */
  ByMethodSpec put(Handler handler);

  /**
   * Defines the action to to take if the request has a HTTP method of PATCH.
   *
   * @param handler The handler to invoke if the request method matches
   * @return this
   */
  ByMethodSpec patch(NoArgAction handler);

  /**
   * Defines the action to to take if the request has a HTTP method of PATCH.
   *
   * @param handler The handler to invoke if the request method matches
   * @return this
   */
  ByMethodSpec patch(Handler handler);

  /**
   * Defines the action to to take if the request has a HTTP method of DELETE.
   *
   * @param handler The handler to invoke if the request method matches
   * @return this
   */
  ByMethodSpec delete(NoArgAction handler);

  /**
   * Defines the action to to take if the request has a HTTP method of DELETE.
   *
   * @param handler The handler to invoke if the request method matches
   * @return this
   */
  ByMethodSpec delete(Handler handler);

  /**
   * Defines the action to to take if the request has a HTTP method of {@code methodName}.
   * <p>
   * The method name is case insensitive.
   *
   * @param methodName The HTTP method to map the given action to
   * @param handler The handler to invoke if the request method matches
   * @return this
   */
  ByMethodSpec named(String methodName, NoArgAction handler);

  /**
   * Defines the action to to take if the request has a HTTP method of {@code methodName}.
   * <p>
   * The method name is case insensitive.
   *
   * @param methodName The HTTP method to map the given action to
   * @param handler The handler to invoke if the request method matches
   * @return this
   */
  ByMethodSpec named(String methodName, Handler handler);

}
