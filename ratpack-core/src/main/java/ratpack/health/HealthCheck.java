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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterables;
import ratpack.api.Nullable;
import ratpack.exec.Promise;
import ratpack.exec.Throttle;
import ratpack.exec.util.ParallelBatch;
import ratpack.func.Function;
import ratpack.func.Pair;
import ratpack.registry.Registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reports on the health of some aspect of the system.
 * <p>
 * Health checks are typically used for reporting and monitoring purposes.
 * The results exposed by health checks can be reported via HTTP by a {@link ratpack.health.HealthCheckHandler}.
 * <p>
 * The actual check is implemented by the {@link #check(Registry)} method, that returns a promise for a {@link Result}.
 * <p>
 * The inspiration for health checks in Ratpack comes from the <a href="https://dropwizard.github.io/metrics/3.1.0/manual/healthchecks/">Dropwizard Metrics</a> library.
 * Ratpack health checks are different in that they support non blocking checks, and all health checks require a unique name.
 *
 * @see ratpack.health.HealthCheckHandler
 */
public interface HealthCheck {

  /**
   * The result of a health check.
   *
   * Instances can be created by one of the static methods of this class (e.g. {@link #healthy()}).
   */
  class Result {

    private static final Result HEALTHY = new Result(true, null, null);

    private final boolean healthy;
    private final String message;
    private final Throwable error;

    private Result(boolean healthy, String message, Throwable error) {
      this.healthy = healthy;
      this.message = message;
      this.error = error;
    }

    /**
     * Was the component being checked healthy?
     *
     * @return {@code true} if application component is healthy
     */
    public boolean isHealthy() {
      return healthy;
    }

    /**
     * Any message provided as part of the check, may be {@code null}.
     * <p>
     * A message may be provided with a healthy or unhealthy result.
     * <p>
     * If {@link #getError()} is non null, this message will be the message provided by that exception.
     *
     * @return any message provided as part of the check, may be {@code null}
     */
    @Nullable
    public String getMessage() {
      return message;
    }

    /**
     * The exception representing an unhealthy check, may be {@code null}.
     * <p>
     * Healthy results will never have an associated error.
     *
     * @return the exception representing an unhealthy check, may be {@code null}
     */
    @Nullable
    public Throwable getError() {
      return error;
    }

    /**
     * Creates a healthy result, with no message.
     *
     * @return a healthy result, with no message.
     */
    public static Result healthy() {
      return HEALTHY;
    }

    /**
     * Creates a healthy result, with the given message.
     *
     * @param message a message to accompany the result
     * @return a healthy result, with the given message
     */
    public static Result healthy(String message) {
      return new Result(true, message, null);
    }

    /**
     * Creates a healthy result, with the given message.
     * <p>
     * The message is constructed by {@link String#format(String, Object...)} and the given arguments.
     *
     * @param message the message format strings
     * @param args values to be interpolated into the format string
     * @return a healthy result, with the given message
     */
    public static Result healthy(String message, Object... args) {
      return healthy(String.format(message, args));
    }

    /**
     * Creates an unhealthy result, with the given message.
     *
     * @param message a message to accompany the result
     * @return an unhealthy result, with the given message
     */
    public static Result unhealthy(String message) {
      return new Result(false, message, null);
    }

    /**
     * Creates an unhealthy result, with the given message.
     * <p>
     * The message is constructed by {@link String#format(String, Object...)} and the given arguments.
     *
     * @param message the message format strings
     * @param args values to be interpolated into the format string
     * @return an unhealthy result, with the given message
     */
    public static Result unhealthy(String message, Object... args) {
      return unhealthy(String.format(message, args));
    }

    /**
     * Creates an unhealthy result, with the given exception.
     * <p>
     * The message of the given exception will also be used as the message of the result.
     *
     * @param error an exception thrown during health check
     * @return an unhealthy result, with the given error
     */
    public static Result unhealthy(Throwable error) {
      return unhealthy(error.getMessage(), error);
    }

    /**
     * Creates an unhealthy result, with the given exception and message.
     * <p>
     * The supplied message will be used as the message of the result.
     *
     * @param message a message to accompany the result
     * @param error an exception thrown during health check
     * @return an unhealthy result, with the given error and message
     * @since 1.2
     */
    public static Result unhealthy(String message, Throwable error) {
      return new Result(false, message, error);
    }
  }

  /**
   * The <b>unique</b> name of the health check.
   * <p>
   * Each health check within an application must have a unique name.
   *
   * @return the name of the health check
   */
  String getName();

  /**
   * Checks the health of the component, providing a promise for the result.
   * <p>
   * This method returns a promise to allow check implementations to be asynchronous.
   * If the implementation does not need to be asynchronous, the result can be returned via {@link Promise#value(Object)}.
   * <p>
   * The {@code registry} argument is the server registry, from which other supporting objects can be obtained.
   * <p>
   * If this method throws an exception, it is logically equivalent to returned an unhealthy result with the thrown exception.
   * <p>
   * If the method returns a failed promise, it will be converted to a result using {@link Result#unhealthy(Throwable)}.
   *
   * @param registry the server registry
   * @return a promise for the result
   * @throws Exception any
   */
  Promise<HealthCheck.Result> check(Registry registry) throws Exception;

  /**
   * Convenience factory for health check implementations.
   * <pre class="java">{@code
   * import ratpack.test.exec.ExecHarness;
   * import ratpack.exec.Promise;
   * import ratpack.registry.Registry;
   * import ratpack.health.HealthCheck;
   * import static org.junit.Assert.*;
   *
   * public class Example {
   *   public static void main(String... args) throws Exception {
   *     HealthCheck.Result result = ExecHarness.yieldSingle(e ->
   *       HealthCheck.of("test", registry ->
   *         Promise.value(HealthCheck.Result.healthy())
   *       ).check(Registry.empty())
   *     ).getValue();
   *
   *     assertTrue(result.isHealthy());
   *   }
   * }
   * }</pre>
   *
   * @param name a name of health check
   * @param func a health check implementation
   * @return a named health check implementation
   */
  static HealthCheck of(String name, Function<? super Registry, ? extends Promise<Result>> func) {
    return new HealthCheck() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public Promise<Result> check(Registry registry) throws Exception {
        return func.apply(registry);
      }
    };
  }

  /**
   * Execute health checks.
   * <p>
   * The checks will be executed in parallel.
   *
   * @param registry the registry to pass to each health check
   * @param healthChecks the health checks to execute
   * @return the results
   * @since 1.5
   */
  static Promise<HealthCheckResults> checkAll(Registry registry, Iterable<? extends HealthCheck> healthChecks) {
    return checkAll(registry, Throttle.unlimited(), healthChecks);
  }

  /**
   * Execute health checks.
   * <p>
   * The checks will be executed in parallel.
   *
   * @param registry the registry to pass to each health check
   * @param throttle the throttle to use to constrain the parallelism of the checks
   * @param healthChecks the health checks to execute
   * @return the results
   * @since 1.5
   */
  static Promise<HealthCheckResults> checkAll(Registry registry, Throttle throttle, Iterable<? extends HealthCheck> healthChecks) {
    return Promise.flatten(() -> {
      ImmutableList<? extends HealthCheck> healthChecksCopy = ImmutableList.copyOf(healthChecks);
      if (healthChecksCopy.isEmpty()) {
        return Promise.value(HealthCheckResults.empty());
      }

      Iterable<Promise<Pair<Result, String>>> resultPromises = Iterables.transform(healthChecksCopy, h ->
        Promise.flatten(() -> h.check(registry))
          .throttled(throttle)
          .mapError(HealthCheck.Result::unhealthy)
          .right(r -> h.getName())
      );

      Map<String, Result> results = new ConcurrentHashMap<>();
      return ParallelBatch.of(resultPromises)
        .forEach((i, result) -> results.put(result.right, result.left))
        .map(() -> new HealthCheckResults(ImmutableSortedMap.copyOf(results)));
    });
  }
}
