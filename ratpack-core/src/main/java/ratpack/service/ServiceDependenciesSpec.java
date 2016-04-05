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

import static ratpack.service.internal.ServicesGraph.isOfType;

/**
 * Allows declaring which services depend on which services.
 *
 * @see ServiceDependencies
 * @since 1.3
 */
public interface ServiceDependenciesSpec {

  /**
   * Specifies that all services that match the {@code dependents} predicate are dependent on all services that match the {@code dependencies} predicate.
   *
   * @param dependents the criteria for dependent services
   * @param dependencies the criteria for services they depend on
   * @return {@code this}
   * @throws Exception any thrown by either predicate
   */
  ServiceDependenciesSpec dependsOn(Predicate<? super Service> dependents, Predicate<? super Service> dependencies) throws Exception;

  /**
   * A convenience form of {@link #dependsOn(Predicate, Predicate)} where the predicates are based on compatibility with the given types.
   * <p>
   * All services that are type compatible with the {@code dependents} type,
   * will be considered dependents of all services that are type compatible with the {@code dependencies} type.
   * <p>
   * Note that this method is {@link LegacyServiceAdapter} aware.
   * Adapted services are unpacked, and their real type (i.e. {@link ratpack.server.Service} implementation type) is used.
   *
   * @param dependents the type of dependent services
   * @param dependencies the type of the services they depend on
   * @return {@code this}
   * @throws Exception any
   */
  default ServiceDependenciesSpec dependsOn(Class<?> dependents, Class<?> dependencies) throws Exception {
    return dependsOn(s -> isOfType(s, dependents), s -> isOfType(s, dependencies));
  }

}
