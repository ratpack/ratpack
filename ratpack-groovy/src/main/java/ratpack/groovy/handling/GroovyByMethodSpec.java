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

package ratpack.groovy.handling;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.handling.ByMethodSpec;

/**
 * A Groovy oriented multi-method handler builder.
 */
public interface GroovyByMethodSpec extends ByMethodSpec {

  /**
   * Inserts the handler to chain if the request has a HTTP method of GET.
   *
   * @param closure a handler closure
   * @return this {@code ByMethodSpec}
   */
  ByMethodSpec get(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure);

  /**
   * Inserts the handler to chain if the request has a HTTP method of POST.
   *
   * @param closure a handler closure
   * @return this {@code ByMethodSpec}
   */
  ByMethodSpec post(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure);

  /**
   * Inserts the handler to chain if the request has a HTTP method of PUT.
   *
   * @param closure a handler closure
   * @return this {@code ByMethodSpec}
   */
  ByMethodSpec put(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure);

  /**
   * Inserts the handler to chain if the request has a HTTP method of PATCH.
   *
   * @param closure a handler closure
   * @return this {@code ByMethodSpec}
   */
  ByMethodSpec patch(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure);


  /**
   * Inserts the handler to chain if the request has a HTTP method of OPTIONS.
   *
   * @param closure a handler closure
   * @return this {@code ByMethodSpec}
   */
  ByMethodSpec options(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure);


  /**
   * Inserts the handler to chain if the request has a HTTP method of DELETE.
   *
   * @param closure a handler closure
   * @return this {@code ByMethodSpec}
   */
  ByMethodSpec delete(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure);

  /**
   * Inserts the handler to chain if the request has a HTTP method of {@code methodName}.
   * @param closure a handler closure
   * @return this {@code ByMethodSpec}
   */
  ByMethodSpec named(@DelegatesTo(value = GroovyContext.class, strategy = Closure.DELEGATE_FIRST) String methodName, Closure<?> closure);

}
