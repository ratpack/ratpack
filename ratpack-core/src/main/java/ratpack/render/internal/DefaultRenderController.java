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

import com.google.common.base.Predicate;
import com.google.common.reflect.TypeToken;
import ratpack.handling.Context;
import ratpack.render.NoSuchRendererException;
import ratpack.render.Renderer;
import ratpack.render.RendererException;

public class DefaultRenderController implements RenderController {

  private static final TypeToken<Renderer<?>> RENDERER_TYPE_TOKEN = new TypeToken<Renderer<?>>() {
    private static final long serialVersionUID = 0;
  };

  @Override
  public void render(final Object toRender, final Context context) throws Exception {
    if (toRender == null) {
      context.clientError(404);
      return;
    }

    Renderer<?> renderer = context.first(RENDERER_TYPE_TOKEN, new RendererForTypePredicate(toRender.getClass()));
    if (renderer == null) {
      throw new NoSuchRendererException(toRender);
    } else {
      try {
        doRender(toRender, renderer, context);
      } catch (Exception e) {
        throw new RendererException(renderer, toRender, e);
      }
    }
  }

  private <T> void doRender(Object object, Renderer<T> renderer, Context context) throws Exception {
    @SuppressWarnings("unchecked") T cast = (T) object;
    renderer.render(context, cast);
  }

  private static class RendererForTypePredicate implements Predicate<Renderer<?>> {
    private final Class<?> toRenderType;

    public RendererForTypePredicate(Class<?> toRenderType) {
      this.toRenderType = toRenderType;
    }

    @Override
    public boolean apply(Renderer<?> renderer) {
      return renderer.getType().isAssignableFrom(toRenderType);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      RendererForTypePredicate that = (RendererForTypePredicate) o;

      return toRenderType.equals(that.toRenderType);
    }

    @Override
    public int hashCode() {
      return toRenderType.hashCode();
    }
  }
}
