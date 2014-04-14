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

package ratpack.registry;

import ratpack.func.Action;
import ratpack.func.Factory;
import ratpack.registry.internal.DefaultRegistryBuilder;
import ratpack.registry.internal.HierarchicalRegistry;

/**
 * Static methods for creating and building {@link ratpack.registry.Registry registries}.
 */
public abstract class Registries {

  private Registries() {
  }

  /**
   * Adds a registry entry that is available by the given type.
   *
   * @param type the public type of the registry entry
   * @param object the actual registry entry
   * @param <O> the public type of the registry entry
   * @return this
   */
  public static <O> RegistryBuilder add(Class<? super O> type, O object) {
    return registry().add(type, object);
  }

  /**
   * Adds a registry entry.
   *
   * @param object the object to add to the registry
   * @return this
   */
  public static RegistryBuilder add(Object object) {
    return registry().add(object);
  }

  /**
   * Adds a lazily created entry to the registry.
   * <p>
   * The factory will be invoked exactly once, when a query is made to the registry of a compatible type of the given type.
   *
   * @param type the public type of the registry entry
   * @param factory the factory for creating the object when needed
   * @param <O> the public type of the registry entry
   * @return this
   */
  public static <O> RegistryBuilder add(Class<O> type, Factory<? extends O> factory) {
    return registry().add(type, factory);
  }

  /**
   * Joins the given registries into a new registry.
   * <p>
   * The returned registry is effectively the union of the two registries, with the {@code child} taking precedence.
   * This means that child entries are effectively “returned first”.
   * <pre class="tested">
   * import ratpack.registry.Registry;
   *
   * import static ratpack.registry.Registries.registry;
   * import static ratpack.registry.Registries.join;
   *
   * public interface Thing { String getName() }
   *
   * public class ThingImpl implements Thing {
   *   private final String name
   *   public ThingImpl(String name) { this.name = name; }
   *   public String getName() { return name; }
   * }
   *
   *
   * Registry child = registry().add(Thing.class, new ThingImpl("child-1")).add(Thing.class, new ThingImpl("child-2")).build();
   * Registry parent = registry().add(Thing.class, new ThingImpl("parent-1")).add(Thing.class, new ThingImpl("parent-2")).build();
   * Registry joined = join(parent, child);
   *
   * assert joined.get(Thing.class).getName() == "child-1";
   *
   * List&lt;Thing&gt; all = joined.getAll(Thing.class);
   * assert all.get(0).getName() == "child-1";
   * assert all.get(1).getName() == "child-2";
   * assert all.get(2).getName() == "parent-1";
   * assert all.get(3).getName() == "parent-2";
   * </pre>
   *
   * @param parent the parent registry
   * @param child the child registry
   * @return a registry which is the combination of the parent and child
   */
  public static Registry join(Registry parent, Registry child) {
    return new HierarchicalRegistry(parent, child);
  }

  /**
   * Creates a single lazily created entry registry, using {@link RegistryBuilder#add(Class, Factory)}.
   *
   * @param publicType the public type of the entry
   * @param factory the factory for the object
   * @param <T> the public type of the entry
   * @return a new single entry registry
   * @see RegistryBuilder#add(Class, Factory)
   */
  public static <T> Registry just(Class<T> publicType, Factory<? extends T> factory) {
    return registry().add(publicType, factory).build();
  }

  /**
   * Creates a single entry registry, using {@link RegistryBuilder#add(Object)}.
   *
   * @param object the entry object
   * @return a new single entry registry
   * @see RegistryBuilder#add(java.lang.Object)
   */
  public static Registry just(Object object) {
    return registry().add(object).build();
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
  public static <T> Registry just(Class<? super T> publicType, T implementation) {
    return registry().add(publicType, implementation).build();
  }

  /**
   * Creates a new {@link RegistryBuilder registry builder}.
   *
   * @return a new registry builder
   * @see RegistryBuilder
   */
  public static RegistryBuilder registry() {
    return new DefaultRegistryBuilder();
  }

  /**
   * Builds a registry from the given action.
   *
   * @param action the action that defines the registry
   * @return a registry created by the given action
   * @throws Exception any thrown by the action
   * @see RegistrySpecAction
   */
  public static Registry registry(Action<? super RegistrySpec> action) throws Exception {
    RegistryBuilder builder = Registries.registry();
    action.execute(builder);
    return builder.build();
  }

}
