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

package ratpack.health;

import com.google.common.collect.ImmutableSortedMap;

/**
 * A type used to render results of non-blocking health checks with {@link ratpack.handling.Context#render(Object) renderable}.
 *
 * @see ratpack.health.HealthCheckHandler
 */
public class HealthCheckResults {
  private final ImmutableSortedMap<String, HealthCheck.Result> results;

  /**
   * Create wrapper for health check results.
   *
   * @param results immutable map of health check name and its result
   */
  public HealthCheckResults(ImmutableSortedMap<String, HealthCheck.Result> results) {
    this.results = results;
  }

  /**
   * Return immutable and sorted map of health checks.
   *
   * @return Immutable sorted map of health name and its {@code HealthCheck.Result}
   */
  public ImmutableSortedMap<String, HealthCheck.Result> getResults() {
    return results;
  }
}
