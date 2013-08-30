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

package org.ratpackframework.render.controller;

import org.ratpackframework.api.NonBlocking;
import org.ratpackframework.api.Nullable;
import org.ratpackframework.handling.Context;

/**
 * A controller for a renderer request, that selects an appropriate renderer.
 * <p>
 * Typically not used directly, but used internally by the {@link Context#render(Object)} method.
 * <p>
 * Use {@link RenderControllers} to create instances.
 */
public interface RenderController {

  /**
   * Attempt to render the given object for the given context.
   *
   * @param context The context in which to perform the render
   * @param object The object to render
   * @param <T> The type of the object that is to be rendered
   * @throws NoSuchRendererException if no suitable renderer can be found
   */
  @NonBlocking
  <T> void render(Context context, T object) throws NoSuchRendererException;

  /**
   * Create a render operation if possible.
   *
   * @param context The context in which the object will be rendered
   * @param object The object that will be rendered
   * @param <T> The type of {@code object}
   * @return A render operation if one can be created, otherwise {@code null}.
   */
  @Nullable
  <T> RenderOperation<?> operation(Context context, T object);

}
