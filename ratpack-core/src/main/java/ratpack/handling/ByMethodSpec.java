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

import ratpack.func.Block;

/**
 * A specification of how to respond to a request, based on the requested method.
 *
 * @see Context#byMethod(ratpack.func.Action)
 */
public interface ByMethodSpec {

  /**
   * Defines the action to to take if the request has a HTTP method of GET.
   *
   * @param block the code to invoke if the request method matches
   * @return this
   */
  ByMethodSpec get(Block block);

  /**
   * Inserts the handler to chain if the request has a HTTP method of GET.
   *
   * @param clazz a handler class
   * @return this
   * @since 1.5
   */
  ByMethodSpec get(Class<? extends Handler> clazz);

  /**
   * Inserts the handler to chain if the request has a HTTP method of GET.
   *
   * @param handler the handler to delegate to
   * @return this
   * @since 1.5
   */
  ByMethodSpec get(Handler handler);

  /**
   * Defines the action to to take if the request has a HTTP method of POST.
   *
   * @param block the code to invoke if the request method matches
   * @return this
   */
  ByMethodSpec post(Block block);

  /**
   * Inserts the handler to chain if the request has a HTTP method of POST.
   *
   * @param clazz a handler class
   * @return this
   * @since 1.5
   */
  ByMethodSpec post(Class<? extends Handler> clazz);

  /**
   * Inserts the handler to chain if the request has a HTTP method of POST.
   *
   * @param handler the handler to delegate to
   * @return this
   * @since 1.5
   */
  ByMethodSpec post(Handler handler);

  /**
   * Defines the action to to take if the request has a HTTP method of PUT.
   *
   * @param block the code to invoke if the request method matches
   * @return this
   */
  ByMethodSpec put(Block block);

  /**
   * Inserts the handler to chain if the request has a HTTP method of PUT.
   *
   * @param clazz a handler class
   * @return this
   * @since 1.5
   */
  ByMethodSpec put(Class<? extends Handler> clazz);

  /**
   * Inserts the handler to chain if the request has a HTTP method of PUT.
   *
   * @param handler the handler to delegate to
   * @return this
   * @since 1.5
   */
  ByMethodSpec put(Handler handler);

  /**
   * Defines the action to to take if the request has a HTTP method of PATCH.
   *
   * @param block the code to invoke if the request method matches
   * @return this
   */
  ByMethodSpec patch(Block block);

  /**
   * Inserts the handler to chain if the request has a HTTP method of PATCH.
   *
   * @param clazz a handler class
   * @return this
   * @since 1.5
   */
  ByMethodSpec patch(Class<? extends Handler> clazz);

  /**
   * Inserts the handler to chain if the request has a HTTP method of PATCH.
   *
   * @param handler the handler to delegate to
   * @return this
   * @since 1.5
   */
  ByMethodSpec patch(Handler handler);

  /**
   * Defines the action to to take if the request has a HTTP method of OPTIONS.
   *
   * @param block the code to invoke if the request method matches
   * @return this
   * @since 1.1
   */
  ByMethodSpec options(Block block);

  /**
   * Inserts the handler to chain if the request has a HTTP method of OPTIONS.
   *
   * @param clazz a handler class
   * @return this
   * @since 1.5
   */
  ByMethodSpec options(Class<? extends Handler> clazz);

  /**
   * Inserts the handler to chain if the request has a HTTP method of OPTIONS.
   *
   * @param handler the handler to delegate to
   * @return this
   * @since 1.5
   */
  ByMethodSpec options(Handler handler);

  /**
   * Defines the action to to take if the request has a HTTP method of DELETE.
   *
   * @param block the code to invoke if the request method matches
   * @return this
   */
  ByMethodSpec delete(Block block);

  /**
   * Inserts the handler to chain if the request has a HTTP method of DELETE.
   *
   * @param clazz a handler class
   * @return this
   * @since 1.5
   */
  ByMethodSpec delete(Class<? extends Handler> clazz);

  /**
   * Inserts the handler to chain if the request has a HTTP method of DELETE.
   *
   * @param handler the handler to delegate to
   * @return this
   * @since 1.5
   */
  ByMethodSpec delete(Handler handler);

  /**
   * Defines the action to to take if the request has a HTTP method of {@code methodName}.
   * <p>
   * The method name is case insensitive.
   *
   * @param methodName The HTTP method to map the given action to
   * @param block the code to invoke if the request method matches
   * @return this
   */
  ByMethodSpec named(String methodName, Block block);

  /**
   * Inserts the handler to chain if the request has a HTTP method of {@code methodName}.
   * <p>
   * The method name is case insensitive.
   *
   * @param methodName The HTTP method to map the given action to
   * @param clazz a handler class
   * @return this
   * @since 1.5
   */
  ByMethodSpec named(String methodName, Class<? extends Handler> clazz);

  /**
   * Inserts the handler to chain if the request has a HTTP method of {@code methodName}.
   * <p>
   * The method name is case insensitive.
   *
   * @param methodName The HTTP method to map the given action to
   * @param handler the handler to delegate to
   * @return this
   * @since 1.5
   */
  ByMethodSpec named(String methodName, Handler handler);

}
