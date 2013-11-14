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

package ratpack.groovy.block;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.api.NonBlocking;
import ratpack.block.Blocking;
import ratpack.groovy.handling.GroovyContext;

/**
 * A Groovy specific subclass of {@link Blocking} that makes using closures more convenient.
 */
public interface GroovyBlocking extends Blocking {

  <T> GroovySuccessOrError<T> block(Closure<T> closure);

  /**
   * A Groovy specific subclass of {@link ratpack.block.Blocking.Success} that makes using closures more convenient.
   *
   * @param <T> The type of object produced by the blocking operation
   */
  interface GroovySuccess<T> extends Success<T> {

    @NonBlocking
    void then(@DelegatesTo(GroovyContext.class) Closure<?> closure);
  }

  /**
   * A Groovy specific subclass of {@link ratpack.block.Blocking.SuccessOrError} that makes using closures more convenient.
   *
   * @param <T> The type of object produced by the blocking operation
   */
  interface GroovySuccessOrError<T> extends GroovySuccess<T>, SuccessOrError<T> {
    GroovySuccess<T> onError(@DelegatesTo(GroovyContext.class) Closure<?> closure);
  }

}
