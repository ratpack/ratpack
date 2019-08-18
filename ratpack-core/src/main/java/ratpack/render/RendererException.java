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

package ratpack.render;

/**
 * Wraps an exception thrown by a renderer while rendering.
 * <p>
 * The cause of this exception is always the exception thrown by the renderer.
 *
 * @see ratpack.handling.Context#render(Object)
 */
public class RendererException extends RenderException {

  private static final long serialVersionUID = 0;

  /**
   * Constructor.
   *
   * @param renderer the renderer that failed
   * @param object the object being rendered
   * @param cause the exception thrown by the renderer
   */
  public RendererException(Renderer<?> renderer, Object object, Throwable cause) {
    this("Renderer '" + renderer + "' failed to render '" + object + "'", cause);
  }

  /**
   * Constructor.
   *
   * @param message the exception message
   * @param cause the exception thrown by the renderer
   * @since 1.5
   */
  public RendererException(String message, Throwable cause) {
    super(message, cause);
  }
}
