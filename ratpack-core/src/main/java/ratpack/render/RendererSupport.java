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

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import ratpack.handling.Context;
import ratpack.util.Types;
import ratpack.util.internal.InternalRatpackError;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * A {@link Renderer} super class that provides a {@link #getType()} implementation based on the generic type of the impl.
 * <p>
 * Implementations need only to declare the type they render as the value for type variable {@code T} and implement {@link #render(ratpack.handling.Context, Object)}.
 * <pre class="java">{@code
 * import ratpack.handling.Context;
 * import ratpack.render.RendererSupport;
 * import ratpack.test.embed.EmbeddedApp;
 *
 * import static org.junit.Assert.assertEquals;
 *
 * public class Example {
 *
 *   // A type of thing to be rendered
 *   static class Thing {
 *     private final String name;
 *
 *     public Thing(String name) {
 *       this.name = name;
 *     }
 *
 *     public String getName() {
 *       return this.name;
 *     }
 *   }
 *
 *   // Renderer implementation
 *   public static class ThingRenderer extends RendererSupport<Thing> {
 *     public void render(Context context, Thing thing) {
 *       context.render("Thing: " + thing.getName());
 *     }
 *   }
 *
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.fromHandlers(c -> c
 *       .register(r -> r.add(new ThingRenderer()))
 *       .all(ctx -> ctx.render(new Thing("foo")))
 *     ).test(httpClient -> {
 *       assertEquals("Thing: foo", httpClient.getText());
 *     });
 *   }
 * }
 * }</pre>
 * <p>
 * An alternative to implementing a render is to make the type to be rendered implement {@link Renderable}.
 *
 * @param <T> The type of object this renderer renders
 */
public abstract class RendererSupport<T> implements Renderer<T> {

  private final Class<T> type;

  protected RendererSupport() {
    TypeToken<T> typeToken = new TypeToken<T>(getClass()) {
    };

    Type type = typeToken.getType();
    if (type instanceof Class) {
      @SuppressWarnings("unchecked") Class<T> rawType = (Class<T>) typeToken.getRawType();
      this.type = rawType;
    } else if (type instanceof ParameterizedType) {
      Iterable<Type> typeArgs = Arrays.asList(((ParameterizedType) type).getActualTypeArguments());
      if (Iterables.any(typeArgs, Predicates.not((t) -> t.getTypeName().equals("?")))) {
        throw new IllegalArgumentException("Invalid renderable type " + type + ": due to type erasure, type parameter T of RendererSupport must be a Class or a parameterized type with '?' for all type variables (e.g. List<?>)");
      }
      this.type = Types.cast(typeToken.getRawType());
    } else {
      throw new InternalRatpackError("Unhandled type for renderer support: " + type.getClass());
    }
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
  abstract public void render(Context ctx, T t) throws Exception;

}
