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

package ratpack.registry;

import com.google.common.base.Predicate;
import com.google.common.reflect.TypeToken;
import ratpack.func.Action;
import ratpack.registry.internal.HierarchicalRegistry;

import java.util.Optional;

/**
 * An object registry.
 * <p>
 * Registries are primarily used for inter {@link ratpack.handling.Handler} communication in request processing.
 * The {@link ratpack.handling.Context} object that handlers operate on implements the {@link ratpack.registry.Registry} interface.
 * <pre class="tested">
 * import ratpack.handling.Handler;
 * import ratpack.handling.Context;
 * import ratpack.registry.Registry;
 * import static ratpack.registry.Registries.just;
 *
 * public class Thing {
 *   private final String name
 *   public Thing(String name) { this.name = name; }
 *   public String getName() { return name; }
 * }
 *
 * public class UpstreamHandler implements Handler {
 *   public void handle(Context context) {
 *     context.next(just(new Thing("foo")));
 *   }
 * }
 *
 * public class DownstreamHandler implements Handler {
 *   public void handle(Context context) {
 *     assert context instanceof Registry;
 *     Thing thing = context.get(Thing.class);
 *     context.render(thing.getName());
 *   }
 * }
 *
 * import ratpack.test.handling.HandlingResult;
 * import ratpack.test.handling.RequestFixture;
 *
 * import static ratpack.handling.Handlers.chain;
 * import static ratpack.func.Action.noop;
 *
 * Handler chain = chain(new UpstreamHandler(), new DownstreamHandler());
 * HandlingResult result = RequestFixture.handle(chain, noop());
 *
 * assert result.rendered(String.class).equals("foo");
 * </pre>
 * <h3>Thread safety</h3>
 * <p>
 * Registry objects are assumed to be thread safe.
 * No external synchronization is performed around registry access.
 * As registry objects may be used across multiple requests, they should be thread safe.
 * <p>
 * Registries that are <b>created</b> per request however do not need to be thread safe.
 */
public interface Registry {

  /**
   * Provides an object of the specified type, or throws an exception if no object of that type is available.
   *
   * @param type The type of the object to provide
   * @param <O> The type of the object to provide
   * @return An object of the specified type
   * @throws NotInRegistryException If no object of this type can be returned
   */
  default <O> O get(Class<O> type) throws NotInRegistryException {
    return get(TypeToken.of(type));
  }

  /**
   * Provides an object of the specified type, or throws an exception if no object of that type is available.
   *
   * @param type The type of the object to provide
   * @param <O> The type of the object to provide
   * @return An object of the specified type
   * @throws NotInRegistryException If no object of this type can be returned
   */
  default <O> O get(TypeToken<O> type) throws NotInRegistryException {
    return maybeGet(type).orElseThrow(() -> new NotInRegistryException(type));
  }

  /**
   * Does the same thing as {@link #get(Class)}, except returns null instead of throwing an exception.
   *
   * @param type The type of the object to provide
   * @param <O> The type of the object to provide
   * @return An object of the specified type, or null if no object of this type is available.
   */
  default <O> Optional<O> maybeGet(Class<O> type) {
    return maybeGet(TypeToken.of(type));
  }

  /**
   * Does the same thing as {@link #get(Class)}, except returns null instead of throwing an exception.
   *
   * @param type The type of the object to provide
   * @param <O> The type of the object to provide
   * @return An optional of an object of the specified type
   */
  <O> Optional<O> maybeGet(TypeToken<O> type);

  /**
   * Returns all of the objects whose declared type is assignment compatible with the given type.
   *
   * @param type the type of objects to search for
   * @param <O> the type of objects to search for
   * @return All objects of the given type
   */
  default <O> Iterable<? extends O> getAll(Class<O> type) {
    return getAll(TypeToken.of(type));
  }

  /**
   * Returns all of the objects whose declared type is assignment compatible with the given type.
   *
   * @param type the type of objects to search for
   * @param <O> the type of objects to search for
   * @return All objects of the given type
   */
  <O> Iterable<? extends O> getAll(TypeToken<O> type);

  /**
   * Returns the first object whose declared type is assignment compatible with the given type and who satisfies the given predicate.
   *
   * @param type the type of object to search for
   * @param predicate a predicate to check objects against
   * @param <T> the type of the object to search for
   * @return An optional of the object of the specified type that satisfied the specified predicate
   */
  <T> Optional<T> first(TypeToken<T> type, Predicate<? super T> predicate);

  /**
   * Returns all of the objects whose declared type is assignment compatible with the given type and who satisfy the given predicate.
   *
   * @param type the type of objects to search for
   * @param predicate a predicate to check objects against
   * @param <T> the type of objects to search for
   * @return All objects of the given type that satisfy the specified predicate
   */
  <T> Iterable<? extends T> all(TypeToken<T> type, Predicate<? super T> predicate);

  /**
   * Calls the given action with each object whose declared type is assignment compatible with the given type and who satisfies the given predicate.
   *
   * @param type the type of object to search for
   * @param predicate a predicate to check objects against
   * @param action an action to call with each matching object
   * @param <T> the type of object to search for
   * @return true if the predicate ever returned true
   * @throws Exception any thrown by {@code action}
   */
  <T> boolean each(TypeToken<T> type, Predicate<? super T> predicate, Action<? super T> action) throws Exception;

  /**
   * Creates a new registry by joining {@code this} registry with the given registry
   * <p>
   * The returned registry is effectively the union of the two registries, with the {@code child} registry taking precedence.
   * This means that child entries are effectively “returned first”.
   * <pre class="java">
   * import ratpack.registry.Registry;
   *
   * import static ratpack.registry.Registries.registry;
   *
   * import java.util.List;
   * import com.google.common.collect.Lists;
   *
   * public class Example {
   *
   *   public static interface Thing {
   *     String getName();
   *   }
   *
   *   public static class ThingImpl implements Thing {
   *     private final String name;
   *
   *     public ThingImpl(String name) {
   *       this.name = name;
   *     }
   *
   *     public String getName() {
   *       return name;
   *     }
   *   }
   *
   *   public static void main(String[] args) {
   *     Registry child = registry().add(Thing.class, new ThingImpl("child-1")).add(Thing.class, new ThingImpl("child-2")).build();
   *     Registry parent = registry().add(Thing.class, new ThingImpl("parent-1")).add(Thing.class, new ThingImpl("parent-2")).build();
   *     Registry joined = parent.join(child);
   *
   *     assert joined.get(Thing.class).getName() == "child-1";
   *     List&lt;Thing&gt; all = Lists.newArrayList(joined.getAll(Thing.class));
   *     assert all.get(0).getName() == "child-1";
   *     assert all.get(1).getName() == "child-2";
   *     assert all.get(2).getName() == "parent-1";
   *     assert all.get(3).getName() == "parent-2";
   *   }
   * }
   * </pre>
   *
   * @param child the child registry
   * @return a registry which is the combination of the {@code this} and the given child
   */
  default Registry join(Registry child) {
    return new HierarchicalRegistry(this, child);
  }
}
