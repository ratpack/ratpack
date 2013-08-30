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
import org.ratpackframework.render.Renderer;

/**
 * Represents a potential render operation.
 * <p>
 * Not typically used directly, but by Ratpack internals.
 *
 * @param <R> The nominated object type of the underlying {@link org.ratpackframework.render.Renderer}
 */
public interface RenderOperation<R> {

  /**
   * The object that will be rendered.
   * <p>
   * This is the normalized to-render object. That is, the return value of {@link org.ratpackframework.render.Renderer#accept(Object)} on the underlying renderer.
   *
   * @return The object to pass to the renderer's {@link org.ratpackframework.render.Renderer#render(org.ratpackframework.handling.Context, Object)} method.
   */
  R getToRender();

  /**
   * The underlying renderer.
   *
   * @return The underlying renderer.
   */
  Renderer<R> getRenderer();

  /**
   * Execute the render.
   */
  @NonBlocking
  void execute();

}
