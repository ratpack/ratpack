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
import ratpack.func.Action;
import ratpack.handling.Context;
import ratpack.render.NoSuchRendererException;
import ratpack.render.Renderer;
import ratpack.render.RendererException;

import static ratpack.util.ExceptionUtils.uncheck;

public class DefaultRenderController implements RenderController {

  @Override
  public void render(final Object object, final Context context) {
    if (object == null) {
      context.clientError(404);
      return;
    }

    try {
      @SuppressWarnings("rawtypes")
      boolean found = context.first(TypeToken.of(Renderer.class), new Predicate<Renderer>() {
        @Override
        public boolean apply(Renderer renderer) {
          return renderer.getType().isInstance(object);
        }
      }, new Action<Renderer>() {
        @Override
        public void execute(Renderer renderer) throws Exception {
          try {
            maybeRender(object, (Renderer<?>) renderer, context);
          } catch (Exception e) {
            throw new RendererException(renderer, object, e);
          }
        }
      });

      if (!found) {
        throw new NoSuchRendererException(object);
      }

    } catch (Exception e) {
      throw uncheck(e);
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
