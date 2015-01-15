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

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.handling.ByMethodSpec;

/**
 * Closure overloads of methods from {@link ratpack.handling.ByContentSpec}.
 *
 * @see ratpack.groovy.handling.GroovyContext#byMethod(groovy.lang.Closure)
 */
public interface GroovyByMethodSpec extends ByMethodSpec {

  /**
   * Defines the action to to take if the request has a HTTP method of GET.
   *
   * @param closure The action to take
   * @return this
   */
  GroovyByMethodSpec get(@DelegatesTo(GroovyContext.class) Closure<?> closure);

  /**
   * Defines the action to to take if the request has a HTTP method of POST.
   *
   * @param closure The action to take
   * @return this
   */
  GroovyByMethodSpec post(@DelegatesTo(GroovyContext.class) Closure<?> closure);

  /**
   * Defines the action to to take if the request has a HTTP method of PUT.
   *
   * @param closure The action to take
   * @return this
   */
  GroovyByMethodSpec put(@DelegatesTo(GroovyContext.class) Closure<?> closure);

  /**
   * Defines the action to to take if the request has a HTTP method of PATCH.
   *
   * @param closure The action to take
   * @return this
   */
  GroovyByMethodSpec patch(@DelegatesTo(GroovyContext.class) Closure<?> closure);

  /**
   * Defines the action to to take if the request has a HTTP method of DELETE.
   *
   * @param closure The action to take
   * @return this
   */
  GroovyByMethodSpec delete(@DelegatesTo(GroovyContext.class) Closure<?> closure);

  /**
   * Defines the action to to take if the request has a HTTP method of {@code methodName}.
   * <p>
   * The method name is case insensitive.
   *
   * @param methodName The HTTP method to map the given action to
   * @param closure The action to take
   * @return this
   */
  GroovyByMethodSpec named(String methodName, @DelegatesTo(GroovyContext.class) Closure<?> closure);

}
