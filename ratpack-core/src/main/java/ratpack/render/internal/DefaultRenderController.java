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

package ratpack.render.internal;

import com.google.common.reflect.TypeToken;
import ratpack.handling.Context;
import ratpack.render.NoSuchRendererException;
import ratpack.render.RenderableDecorator;
import ratpack.render.Renderer;
import ratpack.render.RendererException;
import ratpack.util.Types;

public class DefaultRenderController implements RenderController {

  private static final TypeToken<Renderer<?>> RENDERER_TYPE = new TypeToken<Renderer<?>>() {};
  private static final TypeToken<RenderableDecorator<?>> RENDERABLE_DECORATOR_TYPE = new TypeToken<RenderableDecorator<?>>() {};

  @Override
  public void render(final Object toRender, final Context context) throws Exception {
    if (toRender == null) {
      context.clientError(404);
      return;
    }

    doRender(toRender, context);
  }

  private <T> void doRender(T toRender, Context context) throws Exception {
    Class<T> type = Types.cast(toRender.getClass());

    T decorated = toRender;
    for (RenderableDecorator<?> decorator : context.getAll(RENDERABLE_DECORATOR_TYPE)) {
      if (decorator.getType().isAssignableFrom(type)) {
        RenderableDecorator<T> cast = Types.cast(decorator);
        decorated = cast.decorate(context, decorated);
      }
    }

    Iterable<? extends Renderer<?>> renderers = context.getAll(RENDERER_TYPE);
    for (Renderer<?> renderer : renderers) {
      if (renderer.getType().isAssignableFrom(type)) {
        Renderer<T> cast = Types.cast(renderer);
        try {
          cast.render(context, decorated);
          return;
        } catch (Exception e) {
          throw new RendererException(renderer, decorated, e);
        }
      }
    }

    throw new NoSuchRendererException(decorated);
  }

}
