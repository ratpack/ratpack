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
import ratpack.handling.ByContentSpec;

/**
 * Closure overloads of methods from {@link ratpack.handling.ByContentSpec}.
 *
 * @see ratpack.groovy.handling.GroovyContext#byContent(groovy.lang.Closure)
 */
public interface GroovyByContentSpec extends ByContentSpec {

  /**
   * Specifies that the given action should be used if the client wants content of the given MIME type.
   * This only supports fully-specified content types (no "*" wildcards).
   *
   * @param mimeType The MIME type to register for
   * @param closure The action to take if the content type matches
   * @return this
   */
  GroovyByContentSpec type(String mimeType, @DelegatesTo(GroovyContext.class) Closure<?> closure);

  /**
   * Specifies that the given action should be used if the client wants content of type "text/plain".
   *
   * @param closure The action to take if the content type matches
   * @return this
   */
  GroovyByContentSpec plainText(@DelegatesTo(GroovyContext.class) Closure<?> closure);

  /**
   * Specifies that the given action should be used if the client wants content of type "text/html".
   *
   * @param closure The action to take if the content type matches
   * @return this
   */
  GroovyByContentSpec html(@DelegatesTo(GroovyContext.class) Closure<?> closure);

  /**
   * Specifies that the given action should be used if the client wants content of type "application/json".
   *
   * @param closure The action to take if the content type matches
   * @return this
   */
  GroovyByContentSpec json(@DelegatesTo(GroovyContext.class) Closure<?> closure);

  /**
   * Specifies that the given action should be used if the client wants content of type "application/xml".
   *
   * @param closure The action to take if the content type matches
   * @return this
   */
  GroovyByContentSpec xml(@DelegatesTo(GroovyContext.class) Closure<?> closure);

  /**
   * Specifies that the given action should be used if the client's requested content type cannot be matched with any of the other handlers.
   *
   * @param closure The action to invoke if the content type doesn't match
   * @return this
   */
  GroovyByContentSpec noMatch(@DelegatesTo(GroovyContext.class) Closure<?> closure);

}
