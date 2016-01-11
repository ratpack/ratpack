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

package ratpack.jackson;

import com.fasterxml.jackson.databind.ObjectWriter;
import ratpack.api.Nullable;

/**
 * A {@link ratpack.handling.Context#render renderable object wrapper} for rendering as JSON.
 *
 * @see Jackson#json
 */
public interface JsonRender {

  /**
   * The underlying object to be rendered.
   *
   * @return The underlying object to be rendered.
   */
  Object getObject();

  /**
   * The object writer to use to render the object as JSON.
   * <p>
   * If null, the "default" writer should be used by the renderer.
   *
   * @return The object writer to be used.
   */
  @Nullable
  ObjectWriter getObjectWriter();

  /**
   * The view class to use when rendering the object.
   * <p>
   * If null, no specific view will be used.
   *
   * @return The view class to be used.
   */
  @Nullable
  Class<?> getViewClass();

}
