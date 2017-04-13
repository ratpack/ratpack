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
 * A value type representing the result of running multiple health checks.
 *
 * @see ratpack.health.HealthCheckHandler
 */
public class HealthCheckResults {

  private final ImmutableSortedMap<String, HealthCheck.Result> results;

  private final static HealthCheckResults EMPTY = new HealthCheckResults(ImmutableSortedMap.of());

  /**
   * Empty results.
   *
   * @return empty results.
   * @since 1.5
   */
  public static HealthCheckResults empty() {
    return EMPTY;
  }

  /**
   * Constructor.
   *
   * @param results the results
   */
  public HealthCheckResults(ImmutableSortedMap<String, HealthCheck.Result> results) {
    this.results = results;
  }

  /**
   * The results.
   *
   * @return the results
   */
  public ImmutableSortedMap<String, HealthCheck.Result> getResults() {
    return results;
  }
}
