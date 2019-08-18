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

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterables;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

/**
 * A value type representing the result of running multiple health checks.
 *
 * @see ratpack.health.HealthCheckHandler
 */
public class HealthCheckResults {

  private final ImmutableSortedMap<String, HealthCheck.Result> results;

  private final static HealthCheckResults EMPTY = new HealthCheckResults(ImmutableSortedMap.of());
  private final boolean unhealthy;

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
    this.unhealthy = Iterables.any(results.values(), Predicates.not(HealthCheck.Result::isHealthy));
  }

  /**
   * The results.
   *
   * @return the results
   */
  public ImmutableSortedMap<String, HealthCheck.Result> getResults() {
    return results;
  }

  /**
   * Whether any result is unhealthy.
   *
   * @return whether any result is unhealthy
   * @since 1.5
   */
  public boolean isUnhealthy() {
    return unhealthy;
  }

  /**
   * Writes a string representation of the results to the given writer.
   * <p>
   * This object's {@link #toString()} provides this representation as a string.
   *
   * @param writer the writer to write to.
   * @throws IOException any thrown by {@code writer}
   * @since 1.5
   */
  public void writeTo(Writer writer) throws IOException {
    boolean first = true;
    for (Map.Entry<String, HealthCheck.Result> entry : results.entrySet()) {
      if (first) {
        first = false;
      } else {
        writer.write("\n");
      }

      String name = entry.getKey();
      HealthCheck.Result result = entry.getValue();

      writer.append(name).append(" : ").append(result.isHealthy() ? "HEALTHY" : "UNHEALTHY");

      String message = result.getMessage();
      if (message != null) {
        writer.append(" [").append(message).append("]");
      }

      Throwable error = result.getError();
      if (error != null) {
        writer.append(" [").append(error.toString()).append("]");
      }
    }
  }

  /**
   * Provides a string representation of the results.
   *
   * @return a string representation of the results.
   * @since 1.5
   */
  @Override
  public String toString() {
    StringWriter stringWriter = new StringWriter();
    try {
      writeTo(stringWriter);
    } catch (IOException e) {
      return "HealthCheckResults";
    }
    return stringWriter.toString();
  }
}
