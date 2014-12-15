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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.reflect.TypeToken;
import ratpack.handling.Context;
import ratpack.render.NoSuchRendererException;
import ratpack.render.RenderableDecorator;
import ratpack.render.Renderer;
import ratpack.render.RendererException;
import ratpack.util.Types;

public class DefaultRenderController implements RenderController {

  private final static TypeToken<RenderableDecorator<?>> DECORATOR_TYPE = new TypeToken<RenderableDecorator<?>>() {};
  private final static TypeToken<Renderer<?>> RENDERER_TYPE = new TypeToken<Renderer<?>>() {};

  private final LoadingCache<Class<?>, RendererPredicate> rendererPredicateCache = CacheBuilder.newBuilder().build(new CacheLoader<Class<?>, RendererPredicate>() {
    @Override
    public RendererPredicate load(Class<?> key) throws Exception {
      return new RendererPredicate(key);
    }
  });

  private final LoadingCache<Class<?>, DecoratorPredicate> decoratorPredicateCache = CacheBuilder.newBuilder().build(new CacheLoader<Class<?>, DecoratorPredicate>() {
    @Override
    public DecoratorPredicate load(Class<?> key) throws Exception {
      return new DecoratorPredicate(key);
    }
  });

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

    Iterable<? extends RenderableDecorator<?>> decorators = context.all(DECORATOR_TYPE, decoratorPredicateCache.get(type));
    for (RenderableDecorator<?> decorator : decorators) {
      RenderableDecorator<T> cast = Types.cast(decorator);
      toRender = cast.decorate(context, toRender);
    }

    T decorated = toRender;

    Renderer<?> renderer = context.first(RENDERER_TYPE, rendererPredicateCache.get(type))
      .orElseThrow(() -> new NoSuchRendererException(decorated));

    Renderer<? super T> cast = Types.cast(renderer);
    try {
      cast.render(context, decorated);
    } catch (Exception e) {
      throw new RendererException(renderer, decorated, e);
    }
  }

  private static class DecoratorPredicate implements Predicate<RenderableDecorator<?>> {

    private final Class<?> type;

    public DecoratorPredicate(Class<?> type) {
      this.type = type;
    }

    @Override
    public boolean apply(RenderableDecorator<?> t) {
      return t.getType().equals(type);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      DecoratorPredicate that = (DecoratorPredicate) o;

      return type.equals(that.type);
    }

    @Override
    public int hashCode() {
      return type.hashCode();
    }
  }

  private static class RendererPredicate implements Predicate<Renderer<?>> {
    private final Class<?> type;

    public RendererPredicate(Class<?> type) {
      this.type = type;
    }

    @Override
    public boolean apply(Renderer<?> renderer) {
      return renderer.getType().isAssignableFrom(type);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      RendererPredicate that = (RendererPredicate) o;

      return type.equals(that.type);
    }

    @Override
    public int hashCode() {
      return type.hashCode();
    }
  }

}
