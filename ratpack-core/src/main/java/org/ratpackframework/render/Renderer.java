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

package org.ratpackframework.render;

import org.ratpackframework.api.NonBlocking;
import org.ratpackframework.api.Nullable;
import org.ratpackframework.handling.Context;

/**
 * A renderer is responsible for rendering an object to the response.
 * <p>
 * Renderers are typically not used directly.
 * Instead, handlers simply call the {@link Context#render(Object)} method which is responsible for
 * finding a suitable renderer for the object. See that method for details on making renderers available.
 *
 * @param <T> The type of object that this renderer knows how to render.
 */
public interface Renderer<T> {

  /**
   * Used to detect whether this renderer will/can render the given object.
   * <p>
   * If this renderer can NOT render the given object, this method will return null.
   * <p>
   * This this renderer can render the object, it will return an object of the type that it knows how to work with
   * in its {@link #render(org.ratpackframework.handling.Context, Object)} method which may not be the same object
   * that was given to this method.
   * <p>
   * This is to support renderers that can take a number of different input types.
   * In such a case, this method would wrap the raw input object in some kind of wrapper for the {@link #render(Context, Object)}.
   * <p>
   * As such, implementations can assume that this method is ALWAYS called before {@code render()} and the return value of this method is what is passed to that method.
   * <p>
   * Most implementations simply use the type of the input object to make the determination.
   * The {@link ByTypeRenderer} class provides a base for such implementations.
   *
   * @param object An object to potentially accept to render.
   * @return {@code null} if this renderer will not render the object, an object to pass to {@link #render(Context, Object)} if it can.
   */
  @Nullable
  T accept(Object object);

  /**
   * Render the given object to the response.
   * <p>
   * Calling this method will finalize the processing, sending the response to the client.
   * <p>
   * Any errors that occur during rendering will be sent to {@link Context#error(Exception)}.
   *
   * @param context The context for the operation
   * @param object The object to render, always the result of a call to {@link #accept(Object)} on this renderer
   */
  @NonBlocking
  void render(Context context, T object);

}
