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

/**
 * An object that specifies dependencies between services.
 * <p>
 * This is the programmatic and more flexible version of the {@link DependsOn} annotation.
 * <p>
 * When starting a server, Ratpack will extract all instances of {@link ServiceDependencies} from the server registry.
 * Each will be called with a spec that they can use to define the dependencies between services.
 * <p>
 * Ratpack will ensure that services that are dependencies are started before their dependent services.
 * If any depended on service fails to start, the dependent service will not be started.
 * When stopping the server, all services that depend on a given service are stopped before it.
 *
 * @see DependsOn
 * @see ServiceDependenciesSpec
 * @since 1.3
 */
public interface ServiceDependencies {

  /**
   * Declares service depenencies via the given spec.
   *
   * @param spec the spec of service dependencies
   * @throws Exception any
   */
  void define(ServiceDependenciesSpec spec) throws Exception;

}
