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

package ratpack.render;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.handling.Context;
import ratpack.registry.RegistrySpec;

import java.util.function.BiFunction;

/**
 * Decorates an object before it is {@link Context#render(Object) rendered}.
 * <p>
 * While this type is called <i>renderable</i> decorator, it is not restricted to decorating {@link Renderable} implementations
 * but is used to decorate all types of objects to be rendered.
 * <p>
 * Renderable decorators are able to decorate/transform objects to be rendered.
 * That is, they effectively sit between the call to {@link Context#render(Object)} and {@link Renderer#render(Context, Object)} of the target renderer.
 * All of the decorators that are type compatible with the object to be rendered are invoked cumulatively in the order that they are returned from the context registry.
 * <p>
 * It is not required, but generally advisable for decorators to return a new object instead of mutating the object they receive.
 * <pre class="java">{@code
 * import ratpack.test.embed.EmbeddedApp;
 * import ratpack.render.Renderer;
 * import ratpack.render.RenderableDecorator;
 *
 * import static org.junit.Assert.*;
 *
 * public class Example {
 *   public static void main(String... args) throws Exception {
 *     EmbeddedApp.fromHandlers(chain -> chain
 *       .register(registry -> registry
 *         .add(Renderer.of(Integer.class, (ctx, i) -> ctx.render(i.toString())))
 *         .add(RenderableDecorator.of(Integer.class, (ctx, i) -> i * 2))
 *         .add(RenderableDecorator.of(Integer.class, (ctx, i) -> i * 2))
 *       )
 *       .get(ctx -> ctx.render(1))
 *     ).test(httpClient ->
 *       assertEquals("4", httpClient.getText())
 *     );
 *   }
 * }
 * }</pre>
 * <p>
 * Such decorators are often used to augment the “model” being given to a template rendering engine to include implicit services and so forth.
 * <p>
 * Note that decorators are selected based on the exact runtime type of the object being rendered, that is based on its {@link Object#getClass()}.
 * As such, decorators must advertise to decorate concrete types as opposed to interfaces or super classes.
 * A decorator effectively cannot change the type of the object-to-render.
 *
 * @param <T> the type of object-to-render that this decorator decorates
 */
public interface RenderableDecorator<T> {

  /**
   * Creates a type token for a decorator of objects of the given type.
   *
   * @param type the object-to-render type
   * @param <T> the object-to-render type
   * @return a type token for a decorator of objects of the given type
   */
  static <T> TypeToken<RenderableDecorator<T>> typeOf(Class<T> type) {
    return new TypeToken<RenderableDecorator<T>>(type) {}.where(new TypeParameter<T>() {}, type);
  }

  /**
   * The type of objects that this decorator decorates.
   *
   * @return the type of objects that this decorator decorates
   */
  Class<T> getType();

  /**
   * Decorates the given object on its way to being rendered.
   * <p>
   * Implementations may either mutate the object and return it, or return an entirely new object.
   *
   * @param context the request context
   * @param object the object-to-render
   * @return a promise for the decorated object
   */
  Promise<T> decorate(Context context, T object);

  /**
   * Creates a renderable decorator implementation for the given type that uses the function as decorator.
   * <p>
   * The function must return the renderable, not a promise.
   * If the decoration needs to perform async ops, use {@link #ofAsync(Class, BiFunction)}.
   *
   * @param type the type of object-to-render to decorate
   * @param impl the implementation of the {@link #decorate(Context, Object)} method
   * @param <T> the type of object-to-render to decorate
   * @return a renderable decorator implementation
   */
  static <T> RenderableDecorator<T> of(Class<T> type, BiFunction<? super Context, ? super T, ? extends T> impl) {
    return new RenderableDecorator<T>() {
      @Override
      public Class<T> getType() {
        return type;
      }

      @Override
      public Promise<T> decorate(Context context, T object) {
        return Promise.sync(() -> impl.apply(context, object));
      }
    };
  }

  /**
   * Creates a renderable decorator implementation for the given type that uses the function as decorator.
   * <p>
   * The function must return a promise for the renderable.
   * If the decoration does not need to perform async ops, use {@link #of(Class, BiFunction)}.
   *
   * @param type the type of object-to-render to decorate
   * @param impl the implementation of the {@link #decorate(Context, Object)} method
   * @param <T> the type of object-to-render to decorate
   * @return a renderable decorator implementation
   */
  static <T> RenderableDecorator<T> ofAsync(Class<T> type, BiFunction<? super Context, ? super T, ? extends Promise<T>> impl) {
    return new RenderableDecorator<T>() {
      @Override
      public Class<T> getType() {
        return type;
      }

      @Override
      public Promise<T> decorate(Context context, T object) {
        return impl.apply(context, object);
      }
    };
  }

  /**
   * A registration action, typically used with {@link RegistrySpec#with(Action)}.
   * <p>
   * Registers this object with the type {@code RenderableDecorator<T>} (where {@code T} is the value of {@link #getType()}), not its concrete type.
   *
   * @return a registration action
   */
  default Action<RegistrySpec> register() {
    return (registrySpec) -> registrySpec.add(typeOf(getType()), this);
  }

}
