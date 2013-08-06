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

import org.ratpackframework.handling.Context;
import org.ratpackframework.render.RenderOperation;
import org.ratpackframework.render.Renderer;

public class DefaultRenderOperation<R> implements RenderOperation<R> {

  private final Context context;
  private final Renderer<R> renderer;
  private final R toRender;

  public DefaultRenderOperation(Context context, Renderer<R> renderer, R toRender) {
    this.context = context;
    this.renderer = renderer;
    this.toRender = toRender;
  }

  public R getToRender() {
    return toRender;
  }

  public Renderer<R> getRenderer() {
    return renderer;
  }

  public void execute() {
    getRenderer().render(context, getToRender());
  }
}
