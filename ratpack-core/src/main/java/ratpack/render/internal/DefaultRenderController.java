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

import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import ratpack.exec.Promise;
import ratpack.handling.Context;
import ratpack.render.*;
import ratpack.util.Types;

import java.util.Iterator;

public class DefaultRenderController implements RenderController {

  private static final TypeToken<Renderer<?>> RENDERER_TYPE = Types.intern(new TypeToken<Renderer<?>>() {});
  private static final TypeToken<RenderableDecorator<?>> RENDERABLE_DECORATOR_TYPE = Types.intern(new TypeToken<RenderableDecorator<?>>() {});

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

    Iterator<? extends RenderableDecorator<?>> decorators = Iterables.filter(
      context.getAll(RENDERABLE_DECORATOR_TYPE),
      d -> d.getType().isAssignableFrom(type)
    ).iterator();

    if (decorators.hasNext()) {
      Promise<T> promise = Promise.value(toRender);
      while (decorators.hasNext()) {
        RenderableDecorator<T> cast = Types.cast(decorators.next());
        promise = promise.flatMap(r -> cast.decorate(context, r));
      }
      promise.then(r -> doRender(context, type, r));
    } else {
      doRender(context, type, toRender);
    }
  }

  private <T> void doRender(Context context, Class<T> type, T decorated) {
    Iterable<? extends Renderer<?>> renderers = context.getAll(RENDERER_TYPE);
    for (Renderer<?> renderer : renderers) {
      if (renderer.getType().isAssignableFrom(type)) {
        Renderer<T> cast = Types.cast(renderer);
        try {
          cast.render(context, decorated);
          return;
        } catch (RenderException e) {
          throw e;
        } catch (Exception e) {
          throw new RendererException(renderer, decorated, e);
        }
      }
    }

    throw new NoSuchRendererException(decorated);
  }
}
