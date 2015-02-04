/*
 * Copyright 2014 the original author or authors.
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

package ratpack.codahale.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.ImmutableSortedMap;

/**
 * A {@link ratpack.handling.Context#render(Object) renderable} type for the result of running health checks.
 *
 * @see ratpack.codahale.healthcheck.HealthCheckHandler
 */
public class HealthCheckResults {

  private final ImmutableSortedMap<String, HealthCheck.Result> healthChecks;

  /**
   * Constructor.
   *
   * @param healthChecks the health check results
   */
  public HealthCheckResults(ImmutableSortedMap<String, HealthCheck.Result> healthChecks) {
    this.healthChecks = healthChecks;
  }

  /**
   * The health check results.
   *
   * @return the health check results
   */
  public ImmutableSortedMap<String, HealthCheck.Result> getResults() {
    return healthChecks;
  }

}
