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
import ratpack.render.Renderable;
import ratpack.render.Renderer;
import ratpack.render.RendererSupport;
import ratpack.util.Types;

public class RenderableRenderer extends RendererSupport<Renderable> {

  public static final TypeToken<Renderer<Renderable>> TYPE = Types.intern(new TypeToken<Renderer<Renderable>>() {});

  public static final Renderer<Renderable> INSTANCE = new RenderableRenderer();

  @Override
  public void render(Context ctx, Renderable object) throws Exception {
    object.render(ctx);
  }

}
