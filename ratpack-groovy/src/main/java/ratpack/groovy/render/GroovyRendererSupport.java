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

package ratpack.groovy.render;

import ratpack.core.render.Renderer;
import ratpack.groovy.handling.GroovyContext;
import ratpack.core.handling.Context;
import ratpack.core.render.RendererSupport;

/**
 * A specialisation of {@link RendererSupport} that makes a {@link GroovyContext} available.
 *
 * @param <T> The type of object this renderer renders
 */
public abstract class GroovyRendererSupport<T> extends RendererSupport<T> {

  /**
   * Delegates to {@link #render(GroovyContext, Object)}, wrapping the given {@code context} in a {@link GroovyContext}.
   *
   * @param ctx The context
   * @param object The object to render
   * @throws Exception the exception thrown by {@link #render(GroovyContext, Object)} if any
   */
  @Override
  public final void render(Context ctx, T object) throws Exception {
    render(GroovyContext.from(ctx), object);
  }

  /**
   * Renders the given object to the context.
   *
   * @see Renderer#render(Context, Object)
   * @param context The context to render to
   * @param object The object to render
   * @throws Exception If the object cannot be rendered for any reason
   */
  public abstract void render(GroovyContext context, T object) throws Exception;

}
