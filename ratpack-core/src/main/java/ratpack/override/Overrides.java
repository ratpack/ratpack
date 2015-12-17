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

package ratpack.override;

import com.google.common.collect.Queues;
import io.netty.util.concurrent.FastThreadLocal;
import ratpack.func.Factory;
import ratpack.registry.Registry;
import ratpack.util.Exceptions;

import java.util.Arrays;
import java.util.Deque;
import java.util.Optional;

/**
 * A mechanism for injecting things into an application from the outside and typically during testing.
 * <p>
 * The overrides mechanism exists primarily to allow convenient overriding of behaviour at test time.
 * {@link ratpack.test.MainClassApplicationUnderTest} builds upon this mechanism.
 * It uses the {@link #apply(Registry, Factory)} to register overrides, while starting up the application under test within the given function.
 * <p>
 * Ratpack components are explicitly designed to be aware of overrides.
 * Such components obtain the overrides either via the {@link #get()} method, or via the server registry (e.g. Guice injection).
 * User code does not typically need to be aware of overrides, as overrides are general used to influence upstream configuration.
 * If you do need access to the overrides, prefer obtaining it from the server registry over {@link #get()} as this method is thread sensitive.
 * <p>
 * Actual overrides are implemented as specific classes that are well known to the consumer and producer of overrides.
 * {@link ForcePortOverride} and {@link UserRegistryOverrides} are both examples of such overrides.
 * The consumers of these overrides simply obtain them from the override registry.
 * <p>
 *
 * <pre class="java">{@code
 * import ratpack.server.ServerConfig;
 * import ratpack.override.Overrides;
 * import ratpack.override.ServerConfigOverrides;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import static groovy.util.GroovyTestCase.assertEquals;
 * import static java.util.Collections.singletonMap;
 *
 * public class Example {
 *   public static void main(String[] args) throws Exception {
 *     Overrides overrides = Overrides.of(ServerConfigOverrides.of(s -> s
 *       .props(singletonMap("foo", "overridden!"))
 *     ));
 *
 *     Overrides.apply(overrides, () ->
 *       EmbeddedApp.of(s -> s
 *         .serverConfig(c -> c
 *           .props(singletonMap("foo", "original"))
 *         )
 *         .handlers(c -> c
 *           .get(ctx -> ctx.render(ctx.get(ServerConfig.class).get("/foo", String.class)))
 *         )
 *       )
 *     ).test(testHttpClient ->
 *       assertEquals("overridden!", testHttpClient.getText())
 *     );
 *   }
 * }
 * }</pre>
 *
 * @see ServerConfigOverrides
 * @see ForcePortOverride
 * @see ForceDevelopmentOverride
 * @see UserRegistryOverrides
 * @since 1.2
 */
@SuppressWarnings("JavadocReference")
public final class Overrides {

  private static final FastThreadLocal<Deque<Overrides>> OVERRIDES = new FastThreadLocal<>();

  private final Registry overrides;

  private Overrides(Registry overrides) {
    this.overrides = overrides;
  }

  /**
   * Sets overrides that will be available during execution of the given function from this thread.
   * <p>
   * The given registry will effectively be the value returned by {@link #get()} during the given function,
   * which is executed immediately.
   * <p>
   * The given overrides will only be returned from {@link #get()} if called from the same thread that is calling this method.
   *
   * @param overrides the overrides to make available during the given function
   * @param during the function to execute while the overrides are available.
   * @param <T> the type of result of the given function
   * @return the result of the given function
   * @throws Exception any thrown by {@code during}
   */
  public static <T> T apply(Overrides overrides, Factory<? extends T> during) throws Exception {
    Deque<Overrides> queue = OVERRIDES.get();
    if (queue == null) {
      queue = Queues.newArrayDeque();
      OVERRIDES.set(queue);
    }
    queue.push(overrides);

    try {
      return during.create();
    } finally {
      queue.poll();
      if (queue.isEmpty()) {
        OVERRIDES.remove();
      }
    }
  }

  /**
   * The currently imposed overrides.
   * <p>
   * When called during a call to {@link #apply(Registry, Factory)} from the same thread,
   * returns an overrides registry comprising the registry given to that method.
   * <p>
   * If no overrides have been imposed at call time, the returned registry is effectively empty.
   *
   * @return the currently imposed overrides
   */
  public static Overrides current() {
    return Optional.ofNullable(OVERRIDES.get())
      .map(Deque::peek)
      .orElseGet(Overrides::none);
  }

  /**
   * An empty overrides registry.
   * <p>
   * Possibly useful during testing.
   *
   * @return an empty overrides registry
   */
  public static Overrides none() {
    return of();
  }

  /**
   * Creates an overrides instance backed by the given registry.
   * <p>
   * Possibly useful during testing.
   *
   * @return an overrides instance backed by the given registry
   */
  public static Overrides of(Override... overrides) {
    return of(Arrays.asList(overrides));
  }

  /**
   * Creates an overrides instance backed by the given registry.
   * <p>
   * Possibly useful during testing.
   *
   * @return an overrides instance backed by the given registry
   */
  public static Overrides of(Iterable<? extends Override> overrides) {
    return new Overrides(Exceptions.uncheck(() -> Registry.of(r -> overrides.forEach(r::add))));
  }

  public <T extends Override> Optional<T> get(Class<T> type) {
    return overrides.maybeGet(type);
  }
}
