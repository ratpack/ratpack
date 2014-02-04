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

package ratpack.codahale.metrics;

import com.codahale.metrics.health.HealthCheck;

/**
 * A named application health check.
 * <p>
 * {@link ratpack.codahale.metrics.NamedHealthCheck#getName()} should be used when registering
 * this health check with {@link com.codahale.metrics.health.HealthCheckRegistry}
 *
 * @see ratpack.codahale.metrics.CodaHaleMetricsModule#healthChecks()
 */
public abstract class NamedHealthCheck extends HealthCheck {

  /**
   * The name to register the health check as.
   * @return the health check name
   */
  public abstract String getName();

}

