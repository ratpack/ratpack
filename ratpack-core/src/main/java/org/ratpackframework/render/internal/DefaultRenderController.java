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

package org.ratpackframework.render.internal;

import com.google.common.collect.ImmutableList;
import org.ratpackframework.api.Nullable;
import org.ratpackframework.handling.Context;
import org.ratpackframework.render.NoSuchRendererException;
import org.ratpackframework.render.RenderController;
import org.ratpackframework.render.RenderOperation;
import org.ratpackframework.render.Renderer;

public class DefaultRenderController implements RenderController {

  private final RenderController parent;
  private final ImmutableList<Renderer<?>> renderers;

  public DefaultRenderController(@Nullable RenderController parent, ImmutableList<Renderer<?>> renderers) {
    this.parent = parent;
    this.renderers = renderers;
  }

  public <T> void render(Context context, T object) throws NoSuchRendererException {
    RenderOperation<T, ?> operation = operation(context, object);

    if (operation == null) {
      throw new NoSuchRendererException("No renderer for type '" + object.getClass() + "' available by controller: " + this);
    } else {
      operation.execute();
    }
  }

  public <T> RenderOperation<T, ?> operation(Context context, T object) {
    for (Renderer<?> renderer : renderers) {
      RenderOperation<T, ?> operation = doAccept(context, object, renderer);
      if (operation != null) {
        return operation;
      }
    }

    if (parent != null) {
      return parent.operation(context, object);
    } else {
      return null;
    }
  }

  private <T, R> RenderOperation<T, R> doAccept(Context context, T object, Renderer<R> renderer) {
    R accepted = renderer.accept(object);
    if (accepted == null) {
      return null;
    } else {
      return new DefaultRenderOperation<T, R>(context, renderer, object, accepted);
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (parent != null) {
      sb = new StringBuilder(parent.toString()).append(" -> ");
    }

    sb.append("RenderController[");
    for (int i = 0; i < renderers.size();) {
      sb.append(renderers.get(i));
      if (++i < renderers.size()) {
        sb.append(", ");
      }
    }
    sb.append("]");

    return sb.toString();
  }
}
