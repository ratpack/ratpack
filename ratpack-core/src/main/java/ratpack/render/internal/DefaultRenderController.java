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

import ratpack.handling.Context;
import ratpack.render.NoSuchRendererException;
import ratpack.render.Renderer;
import ratpack.render.RendererException;

import java.util.List;

public class DefaultRenderController implements RenderController {

  @Override
  public void render(Object object, Context context) {
    if (object == null) {
      context.clientError(404);
      return;
    }

    @SuppressWarnings("rawtypes")
    List<Renderer> all = context.getAll(Renderer.class);
    for (Renderer<?> renderer : all) {
      try {
        if (maybeRender(object, renderer, context)) {
          return;
        }
      } catch (Exception e) {
        context.error(new RendererException(renderer, object, e));
      }
    }

    context.error(new NoSuchRendererException("No renderer for object '" + object + "' of type '" + object.getClass() + "'"));
  }

  private <T> boolean maybeRender(Object object, Renderer<T> renderer, Context context) throws Exception {
    if (renderer.getType().isInstance(object)) {
      @SuppressWarnings("unchecked") T cast = (T) object;
      renderer.render(context, cast);
      return true;
    } else {
      return false;
    }
  }

}
