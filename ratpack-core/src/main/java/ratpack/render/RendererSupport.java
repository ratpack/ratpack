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

import com.google.common.reflect.TypeToken;
import ratpack.handling.Context;

/**
 * A {@link Renderer} super class that provides a {@link #getType()} implementation based on the generic type of the impl.
 * <p>
 * Implementations need only to declare the type they render as the value for type variable {@code T} and implement {@link #render(ratpack.handling.Context, Object)}.
 * <pre class="java">{@code
 * import ratpack.handling.Context;
 * import ratpack.render.RendererSupport;
 * import ratpack.test.embed.EmbeddedApp;
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
 *   public static void main(String... args) {
 *     EmbeddedApp.fromChain(c -> c
 *       .register(r -> r.add(new ThingRenderer()))
 *       .handler(ctx -> ctx.render(new Thing("foo")))
 *     ).test(httpClient -> {
 *       assert httpClient.getText().equals("Thing: foo");
 *     });
 *   }
 * }
 * }</pre>
 *
 * @param <T> The type of object this renderer renders
 */
public abstract class RendererSupport<T> implements Renderer<T> {

  private final Class<T> type;

  protected RendererSupport() {
    TypeToken<T> typeToken = new TypeToken<T>(getClass()) {
    };

    if (typeToken.getType() instanceof Class) {
      @SuppressWarnings("unchecked") Class<T> rawType = (Class<T>) typeToken.getRawType();
      this.type = rawType;
    } else {
      throw new IllegalArgumentException("Type parameter T of RendererSupport must be a Class");
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
  abstract public void render(Context context, T t) throws Exception;

}
