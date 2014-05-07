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

  @Override
  public void render(final Object object, Context context) {
    if (object == null) {
      context.clientError(404);
      return;
    }

    @SuppressWarnings("rawtypes")
    Renderer<?> renderer = context.first(TypeToken.of(Renderer.class), new Predicate<Renderer>() {
      @Override
      public boolean apply(Renderer renderer) {
        return renderer.getType().isInstance(object);
      }
    });

    if (renderer == null) {
      throw new NoSuchRendererException(object);
    } else {
      try {
        maybeRender(object, renderer, context);
      } catch (Exception e) {
        throw new RendererException(renderer, object, e);
      }
    }
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
