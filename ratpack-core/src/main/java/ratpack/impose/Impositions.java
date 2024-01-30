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

package ratpack.impose;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import io.netty.util.concurrent.FastThreadLocal;
import ratpack.func.Action;
import ratpack.func.Factory;
import ratpack.registry.Registry;
import ratpack.util.Exceptions;

import java.util.Deque;
import java.util.Map;
import java.util.Optional;

/**
 * A mechanism for imposing things on an application from the outside and typically during testing.
 * <p>
 * The impositions mechanism exists primarily to facilitate convenient overriding of configuration and behaviour at test time.
 * {@link ratpack.test.MainClassApplicationUnderTest} builds upon this mechanism.
 * It uses the {@link #impose(Impositions, Factory)} to register impositions, while starting up the application under test within the given function.
 * <p>
 * Ratpack “components” are explicitly designed to be aware of impositions.
 * Such components obtain the impositions either via the {@link #current()} method, or via the server registry (e.g. Guice injection).
 * User code does not typically need to be aware of impositions, as impositions are general used to influence upstream configuration.
 * If you do need access to the impositions, prefer obtaining it from the server registry over {@link #current()} as this method is thread sensitive.
 * <p>
 * Actual impositions are implemented as specific classes, known to the consumer.
 * {@link ForceServerListenPortImposition} and {@link UserRegistryImposition} are both examples of such.
 * The consumers of these impositions simply obtain them from the impositions registry.
 * <p>
 *
 * <pre class="java">{@code
 * import ratpack.server.ServerConfig;
 * import ratpack.impose.Impositions;
 * import ratpack.impose.ServerConfigImposition;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import static java.util.Collections.singletonMap;
 * import static org.junit.jupiter.api.Assertions.*;
 *
 * public class Example {
 *   public static void main(String[] args) throws Exception {
 *     Impositions.of(i ->
 *       i.add(ServerConfigImposition.of(s -> s .props(singletonMap("foo", "imposed!"))))
 *     ).impose(() ->
 *       EmbeddedApp.of(s -> s
 *         .serverConfig(c -> c
 *           .props(singletonMap("foo", "original"))
 *         )
 *         .handlers(c -> c
 *           .get(ctx -> ctx.render(ctx.get(ServerConfig.class).get("/foo", String.class)))
 *         )
 *       )
 *     ).test(testHttpClient ->
 *       assertEquals("imposed!", testHttpClient.getText())
 *     );
 *   }
 * }
 * }</pre>
 *
 * @see ServerConfigImposition
 * @see ForceServerListenPortImposition
 * @see ForceDevelopmentImposition
 * @see UserRegistryImposition
 * @since 1.2
 */
@SuppressWarnings("JavadocReference")
public final class Impositions {

  private static final FastThreadLocal<Deque<Impositions>> IMPOSITIONS = new FastThreadLocal<>();

  private final Registry registry;

  private Impositions(Registry registry) {
    this.registry = registry;
  }

  /**
   * Sets impositions that will be available during execution of the given function, from this thread.
   * <p>
   * The given impositions will effectively be the value returned by {@link #current()} during the given function,
   * which is executed immediately.
   * <p>
   * The given impositions will only be returned from {@link #current()} if called from the same thread that is calling this method.
   *
   * @param impositions the impositions to impose during the given function
   * @param during the function to execute while the impositions are imposed
   * @param <T> the type of result of the given function
   * @return the result of the given function
   * @throws Exception any thrown by {@code during}
   */
  public static <T> T impose(Impositions impositions, Factory<? extends T> during) throws Exception {
    Deque<Impositions> queue = IMPOSITIONS.get();
    if (queue == null) {
      queue = Queues.newArrayDeque();
      IMPOSITIONS.set(queue);
    }
    queue.addFirst(impositions);

    try {
      return during.create();
    } finally {
      queue.removeFirst();
      if (queue.isEmpty()) {
        IMPOSITIONS.remove();
      }
    }
  }

  /**
   * Sets impositions that will be available during execution of the given function, replacing any existing.
   * <p>
   * The given impositions will only be returned from {@link #current()} if called from the same thread that is calling this method.
   *
   * @param impositions the impositions to impose during the given function
   * @param during the function to execute while the impositions are imposed
   * @param <T> the type of result of the given function
   * @return the result of the given function
   * @throws Exception any thrown by {@code during}
   * @since 1.10
   */
  public static <T> T imposeOver(Impositions impositions, Factory<? extends T> during) throws Exception {
    Deque<Impositions> queue = IMPOSITIONS.get();
    try {
      IMPOSITIONS.set(Queues.newArrayDeque());
      return impose(impositions, during);
    } finally {
      IMPOSITIONS.set(queue);
    }
  }

  /**
   * Delegates to {@link #impose(Impositions, Factory)}, with {@code this} as the impositions.
   *
   * @param during the function to execute while the impositions are imposed
   * @param <T> the type of result of the given function
   * @return the result of the given function
   * @throws Exception any thrown by {@code during}
   */
  public <T> T impose(Factory<? extends T> during) throws Exception {
    return impose(this, during);
  }

  /**
   * Delegates to {@link #imposeOver(Impositions, Factory)}, with {@code this} as the impositions.
   *
   * @param during the function to execute while the impositions are imposed
   * @param <T> the type of result of the given function
   * @return the result of the given function
   * @throws Exception any thrown by {@code during}
   * @since 1.10
   */
  public <T> T imposeOver(Factory<? extends T> during) throws Exception {
    return imposeOver(this, during);
  }

  /**
   * The cumulative currently imposed impositions, for the current thread.
   * <p>
   * When multiple impositions have been applied using layered calls to {@link #impose},
   * the impositions returned here are culmination of all.
   * <p>
   * The {@link #getAll(Class)} used for retrieving certain types of impositions returns objects
   * in order of outer-most-first.
   * This allows a inner-most-wins strategy for impositions of single values (e.g. {@link ForceServerListenPortImposition}).
   * <p>
   * If no impositions have been imposed at call time, the returned impositions object is effectively empty.
   *
   * @return the currently imposed impositions
   */
  public static Impositions current() {
    return Optional.ofNullable(IMPOSITIONS.get())
      .flatMap(impositions -> {
          if (impositions.size() == 1) {
            return Optional.of(impositions.getFirst());
          } else {
            return impositions.stream()
              .map(imposition -> imposition.registry)
              .reduce(Registry::join)
              .map(Impositions::new);
          }
        }
      )
      .orElseGet(Impositions::none);
  }

  /**
   * An empty set of impositions.
   * <p>
   * Possibly useful during testing.
   *
   * @return an empty set of impositions
   */
  public static Impositions none() {
    return Exceptions.uncheck(() -> of(Action.noop()));
  }

  /**
   * Creates an impositions instance of the given imposition objects.
   * <p>
   * Possibly useful during testing.
   *
   * @return an impositions instance of the given imposition objects
   */
  public static Impositions of(Action<? super ImpositionsSpec> consumer) throws Exception {
    Map<Class<? extends Imposition>, Imposition> map = Maps.newHashMap();
    consumer.execute(new ImpositionsSpec() {
      @Override
      public ImpositionsSpec add(Imposition imposition) {
        map.put(imposition.getClass(), imposition);
        return this;
      }
    });
    return new Impositions(Registry.of(r -> map.values().forEach(r::add)));
  }

  /**
   * Return an imposition of the given type, if one is currently imposed.
   *
   * @param type the type of imposition
   * @param <T> the type of imposition
   * @return the imposition of the given type
   * @deprecated since 1.9, use {@link #getAll(Class)}.
   */
  @Deprecated
  public <T extends Imposition> Optional<T> get(Class<T> type) {
    return registry.maybeGet(type);
  }

  /**
   * Return all impositions of the given type.
   *
   * @param type the type of imposition
   * @param <T> the type of imposition
   * @return the impositions of the given type
   * @since 1.9
   */
  public <T extends Imposition> Iterable<? extends T> getAll(Class<T> type) {
    return registry.getAll(type);
  }
}
