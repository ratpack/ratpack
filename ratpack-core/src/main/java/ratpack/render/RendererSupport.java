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

package ratpack.render;

import ratpack.handling.Context;
import ratpack.util.internal.Types;

/**
 * A {@link Renderer} super class that provides a {@link #getType()} implementation based on the generic type of the impl.
 * <p>
 * Implementations need only to declare the type they render as the value for type variable {@code T} and implement {@link #render(ratpack.handling.Context, Object)}.
 * <pre class="tested">
 * // A type of thing to be rendered
 * public class Thing {
 *   private final String name;
 *
 *   public Thing(String name) {
 *     this.name = name;
 *   }
 *
 *   public String getName() {
 *     return this.name;
 *   }
 * }
 *
 * // Renderer implementation
 * public class ThingRenderer extends RendererSupport&lt;Thing$gt; {
 *   public void render(Context context, Thing thing) {
 *     context.render("Thing: " + thing.getName());
 *   }
 * }
 * </pre>
 *
 * @param <T> The type of object this renderer renders
 */
public abstract class RendererSupport<T> implements Renderer<T> {

  private final Class<T> type;

  /**
   * Constructor.
   * <p>
   * Determines the value for {@link #getType()} by reflecting for {@code T}
   */
  protected RendererSupport() {
    this(RendererSupport.class);
  }

  /**
   * Constructor.
   * <p>
   * Only necessary for abstract implementations that propagate the generic type {@code T}.
   * Almost all implementations should use the {@link ratpack.render.RendererSupport() default constructor}.
   *
   * @param type the most specialised parent type of {@code this} that does not have a concrete type for {@code T}
   */
  protected RendererSupport(Class<?> type) {
    this.type = Types.findImplParameterTypeAtIndex(getClass(), type, 0);
  }

  /**
   * The type of object that this renderer can render (the type for {@code T}).
   *
   * @return The type of object that this renderer can render.
   */
  @Override
  public Class<T> getType() {
    return type;
  }

  /**
   * {@inheritDoc}
   */
  abstract public void render(Context context, T object) throws Exception;

}
