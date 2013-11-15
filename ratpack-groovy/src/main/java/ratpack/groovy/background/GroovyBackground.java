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

package ratpack.groovy.background;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.api.NonBlocking;
import ratpack.background.Background;
import ratpack.groovy.handling.GroovyContext;

/**
 * A Groovy specific subclass of {@link ratpack.background.Background} that makes using closures more convenient.
 */
public interface GroovyBackground extends Background {

  <T> GroovySuccessOrError<T> block(Closure<T> closure);

  /**
   * A Groovy specific subclass of {@link ratpack.background.Background.Success} that makes using closures more convenient.
   *
   * @param <T> The type of object produced by the background operation
   */
  interface GroovySuccess<T> extends Success<T> {

    @NonBlocking
    void then(@DelegatesTo(GroovyContext.class) Closure<?> closure);
  }

  /**
   * A Groovy specific subclass of {@link ratpack.background.Background.SuccessOrError} that makes using closures more convenient.
   *
   * @param <T> The type of object produced by the background operation
   */
  interface GroovySuccessOrError<T> extends GroovySuccess<T>, SuccessOrError<T> {
    GroovySuccess<T> onError(@DelegatesTo(GroovyContext.class) Closure<?> closure);
  }

}
