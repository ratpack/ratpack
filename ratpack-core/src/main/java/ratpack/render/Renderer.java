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

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import ratpack.api.NonBlocking;
import ratpack.func.Action;
import ratpack.handling.Context;
import ratpack.registry.RegistrySpec;

/**
 * A renderer is responsible for rendering an object to the response.
 * <p>
 * Renderers are typically not used directly.
 * Instead, they are used via by {@link Context#render(Object)} method.
 * <p>
 * See {@link ratpack.render.RendererSupport} for support base class for implementations.
 *
 * @see ratpack.render.RendererSupport
 * @param <T> The type of object that this renderer knows how to render.
 */
public interface Renderer<T> {

  /**
   * An action that registers this renderer with a registry.
   * <p>
   * Can be used with the {@link RegistrySpec#with(Action)} method of a registry spec.
   *
   * @return an action that registers this renderer with a registry
   */
  default Action<RegistrySpec> register() {
    return (registrySpec) -> registrySpec.add(typeOf(getType()), this);
  }

  /**
   * Creates a type token for a renderer of the given type of object.
   *
   * @param typeToRender the type that the renderer renders
   * @param <T> the type that the renderer renders
   * @return a type token for a renderer of the given type of object
   */
  static <T> TypeToken<Renderer<T>> typeOf(Class<T> typeToRender) {
    return new TypeToken<Renderer<T>>(typeToRender) {}.where(new TypeParameter<T>() {}, typeToRender);
  }

  /**
   * The type of object that this renderer can render.
   *
   * @return The type of object that this renderer can render.
   */
  Class<T> getType();

  /**
   * Render the given object to the response.
   * <p>
   * Calling this method will finalize the processing, sending the response to the client.
   * <p>
   * Any errors that occur during rendering will be sent to {@link Context#error(Throwable)}.
   *
   * @param context the context for the operation
   * @param object the object to render
   * @throws Exception if anything goes wrong while rendering
   */
  @NonBlocking
  void render(Context context, T object) throws Exception;

}
