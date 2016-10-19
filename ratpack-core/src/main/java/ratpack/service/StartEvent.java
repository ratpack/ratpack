/*
 * Copyright 2016 the original author or authors.
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

package ratpack.service;

import ratpack.registry.Registry;

/**
 * A start event.
 *
 * @see Service#onStart(StartEvent)
 * @since 1.3
 */
public interface StartEvent {

  /**
   * The server registry.
   *
   * @return the server registry
   */
  Registry getRegistry();

  /**
   * If the server is starting in response to a reload (during development), as opposed to for the first time.
   *
   * @return if the server is starting in response to a reload
   */
  boolean isReload();

}
