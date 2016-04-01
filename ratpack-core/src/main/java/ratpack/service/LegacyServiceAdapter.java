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

import ratpack.func.Predicate;

/**
 * An adapter from {@link ratpack.server.Service} to {@link ratpack.service.Service}.
 * <p>
 * When using {@link ServiceDependenciesSpec#dependsOn(Predicate, Predicate)}, legacy services will be wrapped in this type.
 * <p>
 * This class is scheduled to be removed in 2.x, along with {@link ratpack.server.Service}.
 *
 * @deprecated since 1.3
 * @since 1.3
 */
@Deprecated
public interface LegacyServiceAdapter {

  /**
   * The legacy service being adapted.
   *
   * @return the legacy service being adapted
   */
  @SuppressWarnings("deprecation")
  ratpack.server.Service getAdapted();

}
