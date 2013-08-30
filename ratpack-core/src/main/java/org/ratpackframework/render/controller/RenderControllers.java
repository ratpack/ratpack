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

import org.ratpackframework.api.Nullable;
import org.ratpackframework.handling.Context;
import org.ratpackframework.render.Renderer;
import org.ratpackframework.render.controller.internal.DefaultRenderController;

/**
 * Factory methods for {@link RenderController} implementations.
 */
import static com.google.common.collect.ImmutableList.copyOf;

public abstract class RenderControllers {

  private RenderControllers() {

  }

  /**
   * Create a render controller from the given renderers.
   *
   * @param renderers the renderers
   * @return a render controller from the given renderers
   */
  public static RenderController renderController(Renderer<?>... renderers) {
    return renderController((RenderController) null, renderers);
  }

  /**
   * Create a render controller from the given renderers.
   *
   * @param renderers the renderers
   * @return a render controller from the given renderers
   */
  public static RenderController renderController(Iterable<? extends Renderer<?>> renderers) {
    return renderController((RenderController) null, renderers);
  }

  /**
   * Create a render controller from the given renderers, using the given context's {@link RenderController} as a parent if it has one.
   * <p>
   * The returned render controller will delegate to the parent controller (if the context has one) if none of the given renderers are suitable.
   *
   * @param context The context to search for a parent controller in
   * @param renderers the renderers
   * @return a render controller from the given renderers
   */
  public static RenderController renderController(Context context, Iterable<? extends Renderer<?>> renderers) {
    return renderController(context.maybeGet(RenderController.class), renderers);
  }

  /**
   * Create a render controller from the given renderers, using the given context's {@link RenderController} as a parent if it has one.
   * <p>
   * The returned render controller will delegate to the parent controller (if the context has one) if none of the given renderers are suitable.
   *
   * @param context The context to search for a parent controller in
   * @param renderers the renderers
   * @return a render controller from the given renderers
   */
  public static RenderController renderController(Context context, Renderer<?>... renderers) {
    return renderController(context.maybeGet(RenderController.class), renderers);
  }

  /**
   * Create a render controller from the given renderers, using the given {@link RenderController} as a parent if it is not {@code null}.
   * <p>
   * The returned render controller will delegate to the parent controller if none of the given renderers are suitable.
   *
   * @param parent The parent render controller to delegate to
   * @param renderers the renderers
   * @return a render controller from the given renderers
   */
  public static RenderController renderController(@Nullable RenderController parent, Renderer<?>... renderers) {
    return renderController(parent, copyOf(renderers));
  }

  /**
   * Create a render controller from the given renderers, using the given {@link RenderController} as a parent if it is not {@code null}.
   * <p>
   * The returned render controller will delegate to the parent controller if none of the given renderers are suitable.
   *
   * @param parent The parent render controller to delegate to
   * @param renderers the renderers
   * @return a render controller from the given renderers
   */
  public static RenderController renderController(@Nullable RenderController parent, Iterable<? extends Renderer<?>> renderers) {
    return new DefaultRenderController(parent, copyOf(renderers));
  }
}
