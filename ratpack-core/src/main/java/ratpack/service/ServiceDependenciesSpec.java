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
import static ratpack.service.internal.ServicesGraph.unpackIfLegacy;

/**
 * Allows declaring which services depend on which services.
 *
 * @see ServiceDependencies
 * @see DependsOn
 * @since 1.3
 */
public interface ServiceDependenciesSpec {

  /**
   * Specifies that all services that match the {@code dependents} predicate are dependent on all services that match the {@code dependencies} predicate.
   * <p>
   * Note that legacy {@link ratpack.server.Service} will be wrapped in a {@link LegacyServiceAdapter} when supplied to the given predicates.
   *
   * @param dependents the criteria for dependent services
   * @param dependencies the criteria for services they depend on
   * @return {@code this}
   * @throws Exception any thrown by either predicate
   */
  ServiceDependenciesSpec dependsOn(Predicate<? super Service> dependents, Predicate<? super Service> dependencies) throws Exception;

  /**
   * Specifies that all services that are of the given {@code dependentsType} that match the {@code dependents} predicate are dependent on all services that are of the {@code dependenciesType} that match the {@code dependencies} predicate.
   * <p>
   * Note that this method is {@link LegacyServiceAdapter} aware.
   * Adapted services are unpacked, and their real type (i.e. {@link ratpack.server.Service} implementation type) is used.
   *
   * @param dependents the criteria for dependent services
   * @param dependencies the criteria for services they depend on
   * @param <T1> the type of dependent services
   * @param <T2> the type of services they depend on
   * @return {@code this}
   * @throws Exception any thrown by either predicate
   */
  default <T1, T2> ServiceDependenciesSpec dependsOn(Class<T1> dependentsType, Predicate<? super T1> dependents, Class<T2> dependenciesType, Predicate<? super T2> dependencies) throws Exception {
    return dependsOn(
      s -> isOfType(s, dependentsType) && dependents.apply(dependentsType.cast(unpackIfLegacy(s))),
      s -> isOfType(s, dependenciesType) && dependencies.apply(dependenciesType.cast(unpackIfLegacy(s)))
    );
  }

  /**
   * A convenience form of {@link #dependsOn(Predicate, Predicate)} where the predicates are based on compatibility with the given types.
   * <p>
   * All services that are type compatible with the {@code dependents} type,
   * will be considered dependents of all services that are type compatible with the {@code dependencies} type.
   * <p>
   * Note that this method is {@link LegacyServiceAdapter} aware.
   * Adapted services are unpacked, and their real type (i.e. {@link ratpack.server.Service} implementation type) is used.
   * <p>
   * Use of this method is equivalent to annotating {@code dependents} with {@link DependsOn} with a value of {@code dependencies}.
   * It can be useful in situations however where you are unable to modify the {@code dependents} class.
   *
   * @param dependents the type of dependent services
   * @param dependencies the type of the services they depend on
   * @return {@code this}
   * @throws Exception any
   * @see DependsOn
   */
  default ServiceDependenciesSpec dependsOn(Class<?> dependents, Class<?> dependencies) throws Exception {
    return dependsOn(dependents, Predicate.TRUE, dependencies, Predicate.TRUE);
  }

}
