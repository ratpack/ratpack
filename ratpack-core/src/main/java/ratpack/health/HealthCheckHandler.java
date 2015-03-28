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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.reflect.TypeToken;
import ratpack.exec.Fulfiller;
import ratpack.exec.Promise;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A handler that executes {@link HealthCheck health checks} and renders the results.
 * <p>
 * The handler obtains health checks via the context's registry.
 * Typically, health checks are added to the server registry.
 * <pre class="java">{@code
 * import ratpack.exec.ExecControl;
 * import ratpack.exec.Promise;
 * import ratpack.health.HealthCheck;
 * import ratpack.health.HealthCheckHandler;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *
 *   static class ExampleHealthCheck implements HealthCheck {
 *     public String getName() {
 *       return "example"; // must be unique within the application
 *     }
 *
 *     public Promise<HealthCheck.Result> check(ExecControl execControl) throws Exception {
 *       return execControl.promiseOf(HealthCheck.Result.healthy());
 *     }
 *   }
 *
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.of(s -> s
 *       .registryOf(r -> r
 *         .add(new ExampleHealthCheck())
 *         .add(HealthCheck.of("inline", execControl -> // alternative way to implement health checks
 *           execControl.promiseOf(HealthCheck.Result.unhealthy("FAILED"))
 *         ))
 *       )
 *       .handlers(c -> c
 *         .get("health/:name?", new HealthCheckHandler())
 *       )
 *     ).test(httpClient -> {
 *       // Run all health checks
 *       String result = httpClient.getText("health");
 *       String[] results = result.split("\n");
 *       assertEquals(2, results.length);
 *       assertEquals("example : HEALTHY", results[0]);
 *       assertEquals("inline : UNHEALTHY [FAILED]", results[1]);
 *
 *       // Run a single health check
 *       result = httpClient.getText("health/example");
 *       assertEquals("example : HEALTHY", result);
 *     });
 *   }
 * }
 * }</pre>
 *
 * <h3>The output format</h3>
 * <p>
 * The handler creates a {@link HealthCheckResults} object with the results of running the health checks and {@link Context#render(Object) renders} it.
 * Ratpack provides a default renderer for {@link HealthCheckResults} objects, that renders results as plain text one per line with the format:
 * <pre>{@code name : HEALTHY|UNHEALTHY [message] [exception]}</pre>
 * <p>
 * To change the output format, simply add your own renderer for this type to the registry.
 * <p>
 * When reporting a single health check, the {@link HealthCheck.Result} object is rendered directly.
 * Ratpack also provides a default renderer for this type.
 *
 * <h3>Concurrency</h3>
 * <p>
 * By default, health checks are executed concurrently.
 * An individual {@link ratpack.exec.Execution} is created for each health check to be executed and all are initiated simultaneously.
 * A {@code concurrency} parameter can be specified when {@link #HealthCheckHandler(String, int) constructing} this handler to alter this behaviour.
 *
 * <h3>Rendering single health checks</h3>
 * <p>
 * The handler checks for the presence of a {@link Context#getPathTokens() path token} to indicate the name of an individual check to execute.
 * By default, the token is named {@value #DEFAULT_NAME_TOKEN}.
 * If a path token is present, but indicates the name of a non-existent health check, a {@code 404} {@link Context#clientError(int) client error} will be raised.
 *
 * @see ratpack.health.HealthCheck
 * @see ratpack.health.HealthCheckResults
 */
public class HealthCheckHandler implements Handler {

  /**
   * The default concurrency level for executing health checks, 0, meaning unlimited concurrency.
   *
   * @see #HealthCheckHandler(String, int)
   */
  public static final int DEFAULT_CONCURRENCY_LEVEL = 0;

  /**
   * The default path token name that indicates the individual health check to run.
   *
   * Value: {@value}
   *
   * @see #HealthCheckHandler(String, int)
   */
  public static final String DEFAULT_NAME_TOKEN = "name";
  private static final TypeToken<HealthCheck> HEALTH_CHECK_TYPE_TOKEN = TypeToken.of(HealthCheck.class);

  private final String name;
  private final int concurrencyLevel;

  /**
   * Uses the default values of {@link #DEFAULT_NAME_TOKEN} and {@link #DEFAULT_CONCURRENCY_LEVEL}.
   *
   * @see #HealthCheckHandler(String, int)
   */
  public HealthCheckHandler() {
    this(DEFAULT_NAME_TOKEN, DEFAULT_CONCURRENCY_LEVEL);
  }

  /**
   * Uses the {@link #DEFAULT_CONCURRENCY_LEVEL} and the given name for the health check identifying path token.
   *
   * @param pathTokenName health check name
   * @see #HealthCheckHandler(String, int)
   */
  public HealthCheckHandler(String pathTokenName) {
    this(pathTokenName, DEFAULT_CONCURRENCY_LEVEL);
  }

  /**
   * Uses the {@link #DEFAULT_NAME_TOKEN} and the given concurrency level.
   *
   * @param concurrencyLevel the concurrency level
   * @see #HealthCheckHandler(String, int)
   */
  public HealthCheckHandler(int concurrencyLevel) {
    this(null, concurrencyLevel);
  }

  /**
   * Constructor.
   * <p>
   * The path token name parameter specifies the name of the path token that, if present, indicates the single health check to execute.
   * If this path token is not present, all checks will be run.
   * <p>
   * The concurrency level indicates  the maximum allowed concurrency of health check execution.
   * <ul>
   * <li><code>&lt; 1</code> - infinite concurrency</li>
   * <li><code>= 1</code> - serialised (i.e. maximum concurrency of 1)</li>
   * <li><code>&gt; 1</code> - bounded concurrency</li>
   * </ul>
   * <p>
   * The actual parallelism of the health check executions is ultimately determined by the size of the application event loop.
   * General, the default value of {@code 0} (i.e. unbounded) is appropriate unless there are many health checks that are executed frequently as this may degrade the performance of other requests.
   *
   * @param pathTokenName the name of health check
   * @param concurrencyLevel the level of concurrency to use when executing health checks
   */
  protected HealthCheckHandler(String pathTokenName, int concurrencyLevel) {
    this.name = pathTokenName;
    this.concurrencyLevel = concurrencyLevel;
  }

  /**
   * The level of concurrency to use when executing health checks.
   *
   * @return the level of concurrency to use when executing health checks
   * @see #HealthCheckHandler(String, int)
   */
  public int getConcurrencyLevel() {
    return concurrencyLevel;
  }

  /**
   * The name of the path token that may indicate a particular health check to execute.
   *
   * @return the name of the path token that may indicate a particular health check to execute
   */
  public String getName() {
    return name;
  }

  /**
   * Renders health checks.
   *
   * @param context the request context
   */
  @Override
  public void handle(Context context) {
    context.getResponse().getHeaders()
      .add("Cache-Control", "no-cache, no-store, must-revalidate")
      .add("Pragma", "no-cache")
      .add("Expires", 0);

    try {
      if (name != null) {
        String checkName = context.getPathTokens().get(name);
        if (checkName != null) {
          handleByName(context, checkName);
          return;
        }
      }

      handleAll(context);
    } catch (Exception e) {
      context.error(e);
    }
  }

  private void handleByName(Context context, String name) throws Exception {
    Optional<HealthCheck> healthCheck = context.first(HEALTH_CHECK_TYPE_TOKEN, it -> it.getName().equals(name) ? it : null);

    if (!healthCheck.isPresent()) {
      context.clientError(404);
      return;
    }
    try {
      Promise<HealthCheck.Result> promise = healthCheck.get().check(context.getExecution().getControl());
      promise.onError(throwable -> {
        context.render(new HealthCheckResults(ImmutableSortedMap.of(healthCheck.get().getName(), HealthCheck.Result.unhealthy(throwable))));
      }).then(r -> {
        context.render(new HealthCheckResults(ImmutableSortedMap.of(healthCheck.get().getName(), r)));
      });
    } catch (Exception ex) {
      context.render(new HealthCheckResults(ImmutableSortedMap.of(healthCheck.get().getName(), HealthCheck.Result.unhealthy(ex))));
    }
  }

  private void handleAll(Context context) throws Exception {
    SortedMap<String, HealthCheck.Result> hcheckResults = new ConcurrentSkipListMap<>();

    SortedMap<String, Promise<HealthCheck.Result>> promises = new ConcurrentSkipListMap<>();
    context.getAll(HealthCheck.class).forEach(hcheck -> {
      try {
        Promise<HealthCheck.Result> promise = hcheck.check(context.getExecution().getControl());
        promises.put(hcheck.getName(), promise);
      } catch (Exception ex) {
        hcheckResults.put(hcheck.getName(), HealthCheck.Result.unhealthy(ex));
      }
    });

    if (promises.size() == 0) {
      context.render(new HealthCheckResults(ImmutableSortedMap.copyOfSorted(hcheckResults)));
      return;
    }

    context.promise(f -> {
      // execute promises in parallel based on the concurrencyLevel
      // count finished health checks. If all are done, render results
      AtomicInteger executedPromisesCountDown = new AtomicInteger(promises.size());
      // count health checks waiting for execution.
      AtomicInteger toExecPromisesCountDown = new AtomicInteger(promises.size());
      // collect health checks to execute in the next run. Used when concurrencyLevel > 1
      Map<String, Promise<HealthCheck.Result>> toExecPromises = new ConcurrentHashMap<>();

      promises.forEach((name, p) -> {
        boolean execParallel = false;
        if (concurrencyLevel <= 0) {
          // execute all health checks in parallel
          execParallel = true;
        } else if (concurrencyLevel == 1) {
          // execute promise by promise, in sequence
          execParallel = false;
        } else {
          // collect promises into group of concurrencyLevel size or into group of last promises to execute
          toExecPromises.put(name, p);
          if (toExecPromisesCountDown.decrementAndGet() > 0 && toExecPromises.size() < concurrencyLevel) {
            return;
          }
          // execute promises in group in parallel
          execParallel = true;
        }

        if (toExecPromises.size() > 0) {
          // promise of immutable map of promises to execute, controls construction and their parallel execution while allows
          // safe freeing of toExecPromises map.
          context.promiseOf(ImmutableMap.copyOf(toExecPromises)).then(map -> {
            // control finalization of parent promise
            AtomicInteger groupOfPromisesCountDown = new AtomicInteger(map.size());
            context.promise(f2 -> {
              map.forEach((name2, p2) -> {
                // execute promise p2 and check end condition: either last promise in group or last promise globally
                execPromiseWithEndCondition(context, name2, p2, f2, hcheckResults, groupOfPromisesCountDown, executedPromisesCountDown);
              });
            }).then(finish -> {
              if (finish == Boolean.TRUE) {
                f.success(hcheckResults);
              }
            });
          });

          toExecPromises.clear();

        } else {
          if (execParallel) {
            // execute promise and if last promise globally, return health check results
            execPromiseWithEndResult(context, name, p, f, hcheckResults, executedPromisesCountDown);
          } else {
            context.promise(f2 -> {
              // execute promise p and check end condition: if last promise globally
              execPromiseWithEndCondition(context, name, p, f2, hcheckResults, null, executedPromisesCountDown);
            }).then(finish -> {
              if (finish == Boolean.TRUE) {
                f.success(hcheckResults);
              }
            });
          }
        }
      });
    }).then(results -> {
      context.render(new HealthCheckResults(ImmutableSortedMap.copyOfSorted(hcheckResults)));
    });
  }

  private void execPromiseWithEndResult(
    Context context,
    String name,
    Promise<HealthCheck.Result> promise,
    Fulfiller<Object> fulfiller,
    SortedMap<String, HealthCheck.Result> hcheckResults,
    AtomicInteger executedPromisesCountDown) {

    context.exec().onComplete(execution -> {
      if (executedPromisesCountDown.decrementAndGet() == 0) {
        fulfiller.success(hcheckResults);
      }
    }).onError(throwable -> {
      hcheckResults.put(name, HealthCheck.Result.unhealthy(throwable));
      if (executedPromisesCountDown.decrementAndGet() == 0) {
        fulfiller.success(hcheckResults);
      }
    }).start(execution -> {
      promise.then(r -> {
        hcheckResults.put(name, r);
      });
    });
  }

  private void execPromiseWithEndCondition(
    Context context,
    String name,
    Promise<HealthCheck.Result> promise,
    Fulfiller<Object> fulfiller,
    SortedMap<String, HealthCheck.Result> hcheckResults,
    AtomicInteger groupOfPromisesCountDown, AtomicInteger executedPromisesCountDown) {

    context.exec().onComplete(execution -> {
      int i = executedPromisesCountDown != null ? executedPromisesCountDown.decrementAndGet() : 0;
      if (groupOfPromisesCountDown != null) {
        if (groupOfPromisesCountDown.decrementAndGet() == 0 || i == 0) {
          fulfiller.success(i == 0 ? Boolean.TRUE : Boolean.FALSE);
        }
      } else {
        fulfiller.success(i == 0 ? Boolean.TRUE : Boolean.FALSE);
      }
    }).onError(throwable -> {
      hcheckResults.put(name, HealthCheck.Result.unhealthy(throwable));
      int i = executedPromisesCountDown != null ? executedPromisesCountDown.decrementAndGet() : 0;
      if (groupOfPromisesCountDown != null) {
        if (groupOfPromisesCountDown.decrementAndGet() == 0 || i == 0) {
          fulfiller.success(i == 0 ? Boolean.TRUE : Boolean.FALSE);
        }
      } else {
        fulfiller.success(i == 0 ? Boolean.TRUE : Boolean.FALSE);
      }
    }).start(execution -> {
      promise.then(r -> {
        hcheckResults.put(name, r);
      });
    });
  }
}
