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

package ratpack.core.server;

import ratpack.exec.api.NonBlocking;

/**
 * Deprecated, replaced by {@link ratpack.core.service.Service}.
 * <p>
 * This class is here to maintain backwards compatibility in Ratpack 1.x.
 * It will be removed in Ratpack 2.x.
 * <p>
 * It is semantically equivalent to {@link ratpack.core.service.Service} in all ways except for ordering.
 * Services of this type do not receive events in of.
 * Services dependencies are implicitly established between services of this type based on their
 * return order from the server registry.
 * <p>
 * It is recommended that all users migrate to the {@link ratpack.core.service.Service} type.
 *
 * @deprecated since 1.3
 * @see ratpack.core.service.Service
 */
@SuppressWarnings("deprecation")
@Deprecated
public interface Service {

  /**
   * See {@link ratpack.core.service.Service#getName()}.
   *
   * @return the name of this service, used for display purposes
   */
  default String getName() {
    return this.getClass().getName();
  }

  /**
   * See {@link ratpack.core.service.Service#onStart(ratpack.core.service.StartEvent)}.
   *
   * @param event meta information about the startup event
   * @throws Exception any
   */
  @NonBlocking
  default void onStart(StartEvent event) throws Exception { }

  /**
   * See {@link ratpack.core.service.Service#onStop(ratpack.core.service.StopEvent)}.
   *
   * @param event meta information about the stop event
   * @throws Exception any
   */
  @NonBlocking
  default void onStop(StopEvent event) throws Exception { }
}
