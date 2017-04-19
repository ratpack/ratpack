/*
 * Copyright 2017 the original author or authors.
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

package ratpack.rocker;

import com.fizzed.rocker.RockerModel;
import ratpack.handling.Context;
import ratpack.render.Renderer;
import ratpack.rocker.internal.DefaultRockerRenderer;

/**
 * A renderer for <a href="https://github.com/fizzed/rocker">Rocker templates</a>.
 * <p>
 * Rocker works by compiling templates into Java classes that extends {@code RockerModel}.
 * Compiling the templates is a build time concern and not handled by this runtime library.
 * Please consult Rocker's documentation regarding build tool plugins for Rocker.
 * <p>
 * To render a Rocker template, simply create an instance of the corresponding {@code RockerModel} class
 * and pass to {@link Context#render(Object)}.
 *
 * @since 1.5
 */
public interface RockerRenderer extends Renderer<RockerModel> {

  /**
   * Creates a {@link Renderer} or {@code RockerModel} objects.
   *
   * @return a {@link Renderer} or {@code RockerModel} objects.
   */
  static Renderer<RockerModel> create() {
    return DefaultRockerRenderer.INSTANCE;
  }

}
