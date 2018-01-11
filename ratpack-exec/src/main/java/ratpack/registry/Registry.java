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

import com.google.common.reflect.TypeToken;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.registry.internal.*;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * An object registry.
 * <p>
 * Registries are primarily used for inter {@link ratpack.handling.Handler} communication in request processing.
 * The {@link ratpack.handling.Context} object that handlers operate on implements the {@link ratpack.registry.Registry} interface.
 * <pre class="tested">
 * import ratpack.handling.Handler;
 * import ratpack.handling.Context;
 * import ratpack.registry.Registry;
 *
 * import static org.junit.Assert.assertTrue;
 *
 * public class Thing {
 *   private final String name
 *   public Thing(String name) { this.name = name; }
 *   public String getName() { return name; }
 * }
 *
 * public class UpstreamHandler implements Handler {
 *   public void handle(Context context) {
 *     context.next(Registry.single(new Thing("foo")));
 *   }
 * }
 *
 * public class DownstreamHandler implements Handler {
 *   public void handle(Context context) {
 *     assertTrue(context instanceof Registry);
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
 * import static org.junit.Assert.assertEquals;
 *
 * Handler chain = chain(new UpstreamHandler(), new DownstreamHandler());
 * HandlingResult result = RequestFixture.handle(chain, noop());
 *
 * assertEquals("foo", result.rendered(String.class));
 * </pre>
 * <h3>Thread safety</h3>
 * <p>
 * Registry objects are assumed to be thread safe.
 * No external synchronization is performed around registry access.
 * As registry objects may be used across multiple requests, they should be thread safe.
 * <p>
 * Registries that are <b>created</b> per request however do not need to be thread safe.
 *
 * <h3>Ordering</h3>
 * <p>
 * Registry objects are returned in the reverse order that they were added (i.e. Last-In-First-Out).
 *
 * <pre class="java">{@code
 * import com.google.common.base.Joiner;
 * import ratpack.registry.Registry;
 *
 * import static org.junit.Assert.assertEquals;
 *
 * public class Example {
 *   public static void main(String... args) throws Exception {
 *     Registry registry = Registry.of(r -> r
 *         .add("Ratpack")
 *         .add("foo")
 *         .add("bar")
 *     );
 *
 *     assertEquals("bar", registry.get(String.class));
 *
 *     String joined = Joiner.on(", ").join(registry.getAll(String.class));
 *     assertEquals("bar, foo, Ratpack", joined);
 *   }
 * }
 * }</pre>
 * <p>
 * While this is strictly the case for the core registry implementations in Ratpack, adapted implementations (e.g. Guice, Spring etc.) may have more nuanced ordering semantics.
 * To the greatest extent possible, registry implementations should strive to honor LIFO ordering.
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
    return get(TypeCaching.typeToken(type));
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
    Optional<O> o = maybeGet(type);
    if (o.isPresent()) {
      return o.get();
    } else {
      throw new NotInRegistryException(type);
    }
  }

  /**
   * Does the same thing as {@link #get(Class)}, except returns null instead of throwing an exception.
   *
   * @param type The type of the object to provide
   * @param <O> The type of the object to provide
   * @return An object of the specified type, or null if no object of this type is available.
   */
  default <O> Optional<O> maybeGet(Class<O> type) {
    return maybeGet(TypeCaching.typeToken(type));
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
    return getAll(TypeCaching.typeToken(type));
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
   * Find and transform an item.
   * <p>
   * This method will apply the given function to items in the order returned by {@link #getAll(TypeToken)} until the function returns a non null value.
   * The first non null value will be returned by this method immediately (as an optional).
   * If the function returns a null value for every item, an empty optional is returned.
   *
   * @param type the type of object to search for
   * @param function a function to apply to each item
   * @param <T> the type of the object to search for
   * @param <O> the type of transformed object
   * @throws Exception any thrown by the function
   * @return An optional of the object of the specified type that satisfied the specified predicate
   */
  default <T, O> Optional<O> first(TypeToken<T> type, Function<? super T, ? extends O> function) throws Exception {
    Iterable<? extends T> all = getAll(type);
    for (T t : all) {
      O out = function.apply(t);
      if (out != null) {
        return Optional.of(out);
      }
    }

    return Optional.empty();
  }

  /**
   * A convenience method for {@link #first(TypeToken, Function)}.
   *
   * @param type the type of object to search for
   * @param function a function to apply to each item
   * @param <T> the type of the object to search for
   * @param <O> the type of transformed object
   * @throws Exception any thrown by the function
   * @return An optional of the object of the specified type that satisfied the specified predicate
   * @see #first(TypeToken, Function)
   */
  default <T, O> Optional<O> first(Class<T> type, Function<? super T, ? extends O> function) throws Exception {
    return first(TypeCaching.typeToken(type), function);
  }

  /**
   * Creates a new registry by joining {@code this} registry with the given registry
   * <p>
   * The returned registry is effectively the union of the two registries, with the {@code child} registry taking precedence.
   * This means that child entries are effectively “returned first”.
   * <pre class="java">{@code
   * import ratpack.registry.Registry;
   *
   * import java.util.List;
   * import com.google.common.collect.Lists;
   *
   * import static org.junit.Assert.assertEquals;
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
   *     Registry child = Registry.builder().add(Thing.class, new ThingImpl("child-1")).add(Thing.class, new ThingImpl("child-2")).build();
   *     Registry parent = Registry.builder().add(Thing.class, new ThingImpl("parent-1")).add(Thing.class, new ThingImpl("parent-2")).build();
   *     Registry joined = parent.join(child);
   *
   *     assertEquals("child-2", joined.get(Thing.class).getName());
   *     List<Thing> all = Lists.newArrayList(joined.getAll(Thing.class));
   *     assertEquals("child-2", all.get(0).getName());
   *     assertEquals("child-1", all.get(1).getName());
   *     assertEquals("parent-2", all.get(2).getName());
   *     assertEquals("parent-1", all.get(3).getName());
   *   }
   * }
   * }</pre>
   *
   * @param child the child registry
   * @return a registry which is the combination of the {@code this} and the given child
   */
  default Registry join(Registry child) {
    if (this == EmptyRegistry.INSTANCE) {
      return child;
    } else if (child == EmptyRegistry.INSTANCE) {
      return this;
    } else {
      return new HierarchicalRegistry(this, child);
    }
  }

  /**
   * Returns an empty registry.
   *
   * @return an empty registry
   */
  static Registry empty() {
    return EmptyRegistry.INSTANCE;
  }

  /**
   * Creates a new {@link RegistryBuilder registry builder}.
   *
   * @return a new registry builder
   * @see RegistryBuilder
   */
  static RegistryBuilder builder() {
    return new DefaultRegistryBuilder();
  }

  /**
   * Builds a registry from the given action.
   *
   * @param action the action that defines the registry
   * @return a registry created by the given action
   * @throws Exception any thrown by the action
   */
  static Registry of(Action<? super RegistrySpec> action) throws Exception {
    RegistryBuilder builder = builder();
    action.execute(builder);
    return builder.build();
  }

  /**
   * Creates a new registry instance that is backed by a RegistryBacking implementation.
   * The registry instance caches lookups.
   *
   * @param registryBacking the implementation that returns instances for the registry
   * @return a new registry
   */
  static Registry backedBy(final RegistryBacking registryBacking) {
    return new CachingBackedRegistry(registryBacking);
  }

  /**
   * Deprecated, use {@link #singleLazy(Class, Supplier)}.
   *
   * @param publicType the public type of the entry
   * @param supplier the supplier for the object
   * @param <T> the public type of the entry
   * @return a new single entry registry
   * @see RegistryBuilder#addLazy(Class, Supplier)
   * @deprecated use {@link #singleLazy(Class, Supplier)}
   */
  @Deprecated
  static <T> Registry single(Class<T> publicType, Supplier<? extends T> supplier) {
    return builder().addLazy(publicType, supplier).build();
  }

  /**
   * Creates a single lazily created entry registry, using {@link RegistryBuilder#addLazy(Class, Supplier)}.
   *
   * @param publicType the public type of the entry
   * @param supplier the supplier for the object
   * @param <T> the public type of the entry
   * @return a new single entry registry
   * @see RegistryBuilder#addLazy(Class, Supplier)
   * @since 1.1
   */
  static <T> Registry singleLazy(Class<T> publicType, Supplier<? extends T> supplier) {
    return builder().addLazy(publicType, supplier).build();
  }

  /**
   * Creates a single entry registry, using {@link RegistryBuilder#add(Object)}.
   *
   * @param object the entry object
   * @return a new single entry registry
   * @see RegistryBuilder#add(java.lang.Object)
   */
  static Registry single(Object object) {
    return builder().add(object).build();
  }

  /**
   * Creates a single entry registry, using {@link RegistryBuilder#add(Class, Object)}.
   *
   * @param publicType the public type of the entry
   * @param implementation the entry object
   * @param <T> the public type of the entry
   * @return a new single entry registry
   * @see RegistryBuilder#add(Class, Object)
   */
  static <T> Registry single(Class<? super T> publicType, T implementation) {
    return builder().add(publicType, implementation).build();
  }

  /**
   * Creates a new, empty, mutable registry.
   * <p>
   * Mutable registries are not concurrent safe when being mutated.
   * Concurrent reading is supported as long as no mutations are occurring.
   *
   * @return a new, empty, mutable registry.
   * @since 1.4
   */
  static MutableRegistry mutable() {
    return new DefaultMutableRegistry();
  }

}
