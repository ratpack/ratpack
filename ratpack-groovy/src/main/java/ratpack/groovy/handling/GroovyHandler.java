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

package ratpack.groovy.handling;

import ratpack.core.handling.Context;
import ratpack.core.handling.Handler;

/**
 * A handler subclass that makes a {@link GroovyContext} available.
 */
public abstract class GroovyHandler implements Handler {

  /**
   * The handle method to implement.
   *
   * @param context The context
   */
  protected abstract void handle(GroovyContext context);

  /**
   * Delegates to {@link #handle(GroovyContext)}.
   *
   * @param context The context
   */
  @Override
  public final void handle(Context context) {
    handle(GroovyContext.from(context));
  }

}
